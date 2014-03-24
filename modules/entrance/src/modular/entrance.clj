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
   [modular.bidi :refer (BidiRoutesContributor routes context)]
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

;; Only users with valid credentials are allowed through a checkpoint.
(defprotocol BidiRouteProtector
  (protect-routes [_ routes]))

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

(defn protect [routes & {:as opts}]
  ["" (->ProtectMatched [routes]
                        (s/validate
                         {:authorizer (s/protocol HttpRequestAuthorizer)
                          (s/optional-key :fail-handler) (s/protocol FailedAuthorizationHandler)}
                         opts))])

(defrecord BidiFailedAuthorizationRedirect [h]
  FailedAuthorizationHandler
  (failed-authorization [_ req]
    (println "FAILED AUTH" (path-for (:modular.bidi/routes req) h))
    (println "FAILED AUTH2" (:modular.bidi/routes req))
    (println "FAILED AUH h" h)
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

;; Since this module is dependent on bidi, let's provide some sample
;; bidi routes that can be used as-is or to demonstrate.

(defn new-login-get-handler [handlers-p post-handler-key]
  (fn [{{{requested-uri :value} "requested-uri"} :cookies
        routes :modular.bidi/routes}]
    {:status 200
     :body
     (html
      [:body
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
        ]])}))

(defn new-login-post-handler [handlers-p get-handler-key {:keys [authorizer http-session-store] :as opts}]
  (s/validate {:authorizer (s/protocol UserPasswordAuthorizer)
               :http-session-store (s/protocol HttpSessionStore)} opts)
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
    @(deliver p {:get-handler (new-login-get-handler p :post-handler)
                 :post-handler (wrap-params (new-login-post-handler p :get-handler opts))})))

;; TODO If a LoginForm component is injected with a login form renderer component, that could be used

(defrecord LoginForm [path context]
  component/Lifecycle
  (start [this]
    (let [handlers (make-login-handlers (select-keys this [:authorizer :http-session-store]))]
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

  BidiRouteProtector
  (protect-routes [this routes]
    (println "protecting routes:" routes)
    (protect routes
             :authorizer (new-session-based-request-authorizer :http-session-store (:http-session-store this))
             :fail-handler (->BidiFailedAuthorizationRedirect (get-in this [:handlers :get-handler])))))

(def new-login-form-schema
  {(s/optional-key :path) s/Str
   (s/optional-key :context) s/Str})

(defn new-login-form [& {:as opts}]
  (let [{:keys [path context]}
        (->> opts
             (merge {:context ""
                     :path "/login"})
             (s/validate new-login-form-schema))]
    (component/using (->LoginForm path context) [:authorizer :http-session-store])))

;; Now we can build a protection domain, composed of a login form, user
;; authorizer and session store. Different constructors can build this
;; component in different ways.

(defrecord ProtectionDomain [login authorizer http-session-store]
  component/Lifecycle
  (start [this] (component/start-system this (keys this)))
  (stop [this] (component/stop-system this (keys this)))
  ;; We must export the routes provided by the login sub-component TODO:
  ;; It's possible that the authorizer and http-session-store components
  ;; provided may also contribute routes - we should return an
  ;; aggregation of all the sub-components, not just the login.
  BidiRoutesContributor
  (routes [this] (routes (:login this)))
  (context [this] (context (:login this))))

(def new-protection-domain-schema {(s/optional-key :session-timeout-in-seconds) s/Int})

(defn new-protection-domain [cfg]
  (s/validate new-protection-domain-schema cfg)
  (map->ProtectionDomain
   {:login (new-login-form)
    :authorizer (new-map-backed-user-registry {"malcolm" "password"})
    :http-session-store (new-atom-backed-session-store
                         (or (:session-timeout-in-seconds cfg)
                             10))}))
