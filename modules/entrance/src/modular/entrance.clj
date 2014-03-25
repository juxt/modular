;; Copyright © 2014, JUXT LTD. All Rights Reserved.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns modular.entrance
  (:require
   [bidi.bidi :as bidi :refer (path-for resolve-handler unresolve-handler ->WrapMiddleware)]
   [modular.bidi :as modbidi :refer (BidiRoutesContributor routes context)]
   [schema.core :as s]
   [ring.middleware.cookies :refer (wrap-cookies)]
   [ring.middleware.params :refer (wrap-params)]
   [hiccup.core :refer (html)]
   [com.stuartsierra.component :as component])
  (:import
   (javax.xml.bind DatatypeConverter)))

(defprotocol HttpRequestAuthorizer
  ;; Return the request, modified if necessary, if the request is authorized
  (authorized-request? [_ request]))

(extend-protocol HttpRequestAuthorizer
  Boolean
  (authorized-request? [this request]
    (when this request)))

(defprotocol UserPasswordAuthorizer
  (authorized-user? [_ user password]))

(extend-protocol UserPasswordAuthorizer
  Boolean
  (authorized-user? [this user password] this))

(defprotocol FailedAuthorizationHandler
  (failed-authorization [_ request]))

;; Certain objects can provide protection for routes
(defprotocol BidiRoutesProtector
  (protect-bidi-routes [_ routes]))

;; Tag that a BidiRoutesContributor is protected
;; TODO Rename BidiRoutesContributor to BidiRoutesProvider
;;(defprotocol BidiRoutesProtected)

(defn wrap-authorization
  "Ring middleware to pre-authorize a request through an authorizer. If
given, the failure-handler is given the request to handle in the event
that authorization fails."
  ([h authorizer failure-handler]
     (fn [req]
       (if-let [mreq (authorized-request? authorizer req)]
         (h mreq)
         (if failure-handler
           (failed-authorization failure-handler req)
           (throw (ex-info {:request (select-keys req :headers :cookies)
                            :authorizer authorizer}))))))
  ([h authorizer]
     (wrap-authorization h authorizer nil)))

;; bidi (https://github.com/juxt/bidi) is required for the functions below

(defn make-authorization-wrapper
  "Currently bidi's WrapMiddleware only allows middleware with a single
  handler argument. This function provides an adapter."
  [authorizer failure-handler]
  (fn [h]
    (wrap-authorization h authorizer failure-handler)))

(defrecord ProtectMatched [routes opts]
  bidi/Matched
  (resolve-handler [this m]
    (let [r (resolve-handler routes m)]
      (if (:handler r)
        (update-in r [:handler]
                   (comp wrap-cookies
                         (make-authorization-wrapper (:authorizer opts) (:fail-handler opts))))
        r)))
  (unresolve-handler [this m]
    (unresolve-handler routes m)))

(defn add-bidi-protection-wrapper [routes & {:as opts}]
  ["" (->ProtectMatched
       [routes]
       (s/validate
        {:authorizer (s/protocol HttpRequestAuthorizer)
         (s/optional-key :fail-handler) (s/protocol FailedAuthorizationHandler)}
        opts))])

(defrecord BidiFailedAuthorizationRedirect [h]
  FailedAuthorizationHandler
  (failed-authorization [_ req]
    {:status 302
     :headers {"Location" (path-for (:modular.bidi/routes req) h)}
     :body "Not authorized\n"
     :cookies {"requested-uri" (:uri req)}}))

(defrecord MapBackedUserRegistry [m]
  UserPasswordAuthorizer
  (authorized-user? [_ user password]
    ((set (seq m)) [user password])))

(defn new-map-backed-user-registry [m]
  (->MapBackedUserRegistry m))

;; Sessions

(defprotocol HttpSessionStore
  (start-session! [_ username]) ; return cookie map compatible with wrap-cookies
  (get-session [_ request]))

(defrecord AtomBackedSessionStore [expiry-seconds]
  component/Lifecycle
  (start [this] (assoc this :sessions (atom {})))
  (stop [this] (dissoc this :sessions))
  HttpSessionStore
  (start-session! [this username]
    (let [uuid (str (java.util.UUID/randomUUID))]
      (swap! (:sessions this)
             assoc uuid
             {:username username
              :expiry (+ (.getTime (java.util.Date.)) (* expiry-seconds 1000))})
      ;; TODO: Make this cookie name configurable
      {"session" {:value uuid
                  :max-age (* expiry-seconds 1000)}}))
  (get-session [this cookies]
    (when-let [{:keys [expiry] :as session} (->> (get cookies "session") :value (get @(:sessions this)))]
      (when (< (.getTime (java.util.Date.)) expiry)
        session))))

(defn new-atom-backed-session-store [expiry-seconds]
  (->AtomBackedSessionStore expiry-seconds))

;; A request authoriser that uses HTTP basic auth

(defrecord HttpBasicRequestAuthorizer [authorizer]
  HttpRequestAuthorizer
  (authorized-request? [_ request]
    (when-let [auth (get-in request [:headers "authorization"])]
      (when-let [basic-creds (second (re-matches #"\QBasic\E\s+(.*)" auth))]
        (let [[username password] (->> (String. (DatatypeConverter/parseBase64Binary basic-creds) "UTF-8")
                                       (re-matches #"(.*):(.*)")
                                       rest)]
          (when (authorized-user? authorizer username password)
            (assoc request :username username)))))))

(defn new-http-based-request-authorizer [& {:as opts}]
  (let [{dlg :user-password-authorizer}
        (s/validate {:user-password-authorizer (s/protocol UserPasswordAuthorizer)} opts)]
    (->HttpBasicRequestAuthorizer dlg)))

;; A request authorizer that uses cookie-based sessions

(defrecord SessionBasedRequestAuthorizer [sessions]
  HttpRequestAuthorizer
  (authorized-request? [_ request]
    (when-let [session (get-session sessions (:cookies request))]
      (assoc request
        :session session
        :username (:username session)))))

(defn new-session-based-request-authorizer [& {:as opts}]
  (let [{dlg :http-session-store}
        (s/validate {:http-session-store (s/protocol HttpSessionStore)} opts)]
    (->SessionBasedRequestAuthorizer dlg)))


;; A request authorizer that tries multiple authorizers in turn

(defrecord CompositeDisjunctiveRequestAuthorizer [delegates]
  HttpRequestAuthorizer
  (authorized-request? [_ request]
    (some #(authorized-request? % request) delegates)))

(defn new-composite-disjunctive-request-authorizer [& delegates]
  (->CompositeDisjunctiveRequestAuthorizer (s/validate [(s/protocol HttpRequestAuthorizer)] delegates)))

;; For a REST API, it is useful to support both HTTP Basic
;; Authentication (for machines) but to honor cookies passed from a
;; browser in an AJAX call, when the user has logged in via a login
;; form.

#_(defrecord ApiAuthorizer []
  component/Lifecycle
  (start [this]
    (let [sessions (get this :http-session-store)
          users (get this :user-registry)]
      (assoc this :authorizer (new-composite-disjunctive-request-authorizer
                               (new-session-based-request-authorizer :http-session-store sessions)
                               (new-http-based-request-authorizer :user-password-authorizer users)))))
  (stop [this] this))

#_(defn new-api-authorizer []
  (component/using (->ApiAuthorizer) [:authorizer :http-session-store]))

;; Since this module is dependent on bidi, let's provide some sample
;; bidi routes that can be used as-is or to demonstrate.

(defn new-login-get-handler [handlers-p post-handler-key {:keys [boilerplate] :as opts}]
  (fn [{{{requested-uri :value} "requested-uri"} :cookies
        routes :modular.bidi/routes}]
    (let [form
          [:form {:method "POST" :style "border: 1px dotted #555"
                  :action (bidi/path-for routes (get @handlers-p post-handler-key))}
           (when requested-uri
             [:input {:type "hidden" :name :requested-uri :value requested-uri}])
           [:div
            [:label {:for "username"} "Username"]
            [:input {:id "username" :name "username" :type "input"}]]
           [:div
            [:label {:for "password"} "Password"]
            [:input {:id "password" :name "password" :type "password"}]]
           [:input {:type "submit" :value "Login"}]
           ]]
      {:status 200
       :body (if boilerplate (boilerplate (html form)) (html [:body form]))})))

(defn new-login-post-handler [handlers-p get-handler-key {:keys [authorizer http-session-store] :as opts}]
  (s/validate {:authorizer (s/protocol UserPasswordAuthorizer)
               :http-session-store (s/protocol HttpSessionStore)}
              opts)
  (fn [{{username "username" password "password" requested-uri "requested-uri"} :form-params
        routes :modular.bidi/routes}]

    (if (and username
             (not-empty username)
             (authorized-user? authorizer (.trim username) password))

      {:status 302
       :headers {"Location" requested-uri}
       :cookies (start-session! http-session-store username)}

      ;; Return back to login form
      {:status 302
       :headers {"Location" (path-for routes (get @handlers-p get-handler-key))}})))

(defn- make-login-handlers [opts]
  (let [p (promise)]
    @(deliver p {:get-handler (new-login-get-handler p :post-handler (select-keys opts [:boilerplate]))
                 :post-handler (wrap-params (new-login-post-handler p :get-handler (select-keys opts [:authorizer :http-session-store])))})))

(defrecord LoginForm [path context boilerplate]
  component/Lifecycle
  (start [this]
    (let [handlers (make-login-handlers (select-keys this [:authorizer :http-session-store :boilerplate]))]
      (assoc this
        :handlers handlers
        :routes [path (->WrapMiddleware
                       {:get (:get-handler handlers)
                        :post (:post-handler handlers)}
                       wrap-cookies)])))
  (stop [this] this)

  BidiRoutesContributor
  (routes [this] (:routes this))
  (context [this] context)

  BidiRoutesProtector
  (protect-bidi-routes [this routes]
    (add-bidi-protection-wrapper
     routes
     :authorizer (new-session-based-request-authorizer :http-session-store (:http-session-store this))
     :fail-handler (->BidiFailedAuthorizationRedirect (get-in this [:handlers :get-handler])))))

(def new-login-form-schema
  {(s/optional-key :path) s/Str
   (s/optional-key :context) s/Str
   (s/optional-key :boilerplate) (s/=> 1)})

(defn new-login-form [& {:as opts}]
  (let [{:keys [path context boilerplate]}
        (->> opts
             (merge {:context ""
                     :path "/login"
                     :boilerplate #(html [:body %])})
             (s/validate new-login-form-schema))]
    (component/using (->LoginForm path context boilerplate) [:authorizer :http-session-store])))

;; Now we can build a protection domain, composed of a login form, user
;; authorizer and session store. Different constructors can build this
;; component in different ways.

(defrecord ProtectionDomain [protector authorizer http-session-store]
  component/Lifecycle
  (start [this] (component/start-system this (keys this)))
  (stop [this] (component/stop-system this (keys this)))
  ;; In this implementation, we export any routes provided by sub-components
  BidiRoutesContributor
  (routes [this] ["" (vec (keep #(when (satisfies? BidiRoutesContributor %) (routes %)) (vals this)))])
  (context [this] (or
                   (first (keep #(when (satisfies? BidiRoutesContributor %) (context %))
                                ((juxt :protector :authorizer :http-session-store) this)))
                   "")))

(def new-default-protection-domain-schema
  {(s/optional-key :session-timeout-in-seconds) s/Int
   (s/optional-key :boilerplate) (s/=> 1)})

(defn new-default-protection-domain [opts]
  (s/validate new-default-protection-domain-schema opts)
  (map->ProtectionDomain
   {:protector (if-let [boilerplate (:boilerplate opts)]
                 (new-login-form :boilerplate boilerplate)
                 (new-login-form))
    :authorizer (new-map-backed-user-registry {"malcolm" "password"})
    :http-session-store (new-atom-backed-session-store
                         (or (:session-timeout-in-seconds opts)
                             10))}))


;; Now that we have a protection domain, we want the ability to create
;; routes components that can be protected.

(defrecord ProtectedBidiRoutes [routes context]
  component/Lifecycle
  (start [this]
    (let [protector (get-in this [:protection-domain :protector])
          routes (cond-> routes
                         (fn? routes) (apply [this])
                         protector ((partial protect-bidi-routes protector)))]
      (assoc this :routes routes)))
  (stop [this] this)

  BidiRoutesContributor
  (routes [this] (:routes this))
  (context [this] context))

(defn new-mandatory-protected-bidi-routes
  "Create a set of protected routes. The absence of a :protection-domain
  dependency will cause an error. Routes can a bidi route structure, or
  a function that takes the component and returns a bidi route
  structure."
  [routes context]
  (component/using (->ProtectedBidiRoutes routes context) [:protection-domain]))

(defn new-protected-bidi-routes
  "Create a set of protected routes. The absence of a :protection-domain
  dependency will remove the protection. If protection is mandatory, use
  new-mandatory-protected-bidi-routes instead. Routes can a bidi route
  structure, or a function that takes the component and returns a bidi
  route structure."  [routes context]
  (->ProtectedBidiRoutes routes context))
