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

(ns modular.cylon
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :refer (pprint)]
   [bidi.bidi :as bidi :refer (path-for resolve-handler unresolve-handler ->WrapMiddleware)]
   [modular.bidi :as modbidi :refer (BidiRoutesContributor routes context)]
   [schema.core :as s]
   [ring.middleware.cookies :refer (wrap-cookies)]
   [ring.middleware.params :refer (wrap-params)]
   [hiccup.core :refer (html)]
   [com.stuartsierra.component :as component])
  (:import
   java.security.SecureRandom
   javax.crypto.SecretKeyFactory
   javax.crypto.spec.PBEKeySpec
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

(def PASSWORD_HASH_ALGO "PBKDF2WithHmacSHA1")

(defn pbkdf2
  "Get a hash for the given string and optional salt. From
http://adambard.com/blog/3-wrong-ways-to-store-a-password/"
  ([password salt]
     (assert password "No password!")
     (assert salt "No salt!")
     (let [k (PBEKeySpec. (.toCharArray password) (.getBytes salt) 1000 192)
           f (SecretKeyFactory/getInstance PASSWORD_HASH_ALGO)]
       (format "%x"
               (java.math.BigInteger. (.getEncoded (.generateSecret f k)))))))

(defn make-salt
  "Make a base64 string representing a salt. Pass in a SecureRandom."
  [rng]
  (let [ba (byte-array 32)]
    (.nextBytes rng ba)
    (javax.xml.bind.DatatypeConverter/printBase64Binary ba)))

(defn create-hash [rng password]
  (let [salt (make-salt rng)
        hash (pbkdf2 password salt)]
    {:salt salt
     :hash hash}))

(defn verify-password [password {:keys [hash salt]}]
  (= (pbkdf2 password salt) hash))

;; ----------------

(defprotocol PasswordStore
  ;; Returns a map of :hash and :salt
  (get-hash-for-uid [_ uid])
  (store-user-hash! [_ uid hash]))

(defrecord PasswordFile [f]
  component/Lifecycle
  (start [this]
    (assoc this
      :ref (ref (if (.exists f) (read-string (slurp f)) {}))
      :agent (agent f)))
  (stop [this] this)
  PasswordStore
  (get-hash-for-uid [this uid] (get @(:ref this) uid))
  (store-user-hash! [this uid hash]
    (dosync
     (alter (:ref this) assoc uid hash)
     (send-off (:agent this) (fn [f]
                               ;;(println "Writing passwords:" @(:ref this) "to" f)
                               (spit f (with-out-str (pprint @(:ref this))))
                               f))
     (get @(:ref this) uid))))

(defn new-password-file [f]
  (assert (.exists (.getParentFile f))
          (format "Please create the directory structure which should contain the password file: %s" f))
  (->PasswordFile f))

(defprotocol NewUserCreator
  (add-user! [_ uid pw]))

;; This implementation of a user domain provides a password storage
;; facility based on PASSWORD_HASH_ALGO and a pluggable store for
;; persistence

(defrecord UserDomain []
  component/Lifecycle
  (start [this] (assoc this :rng (SecureRandom.)))
  (stop [this] this)

  UserPasswordAuthorizer
  (authorized-user? [this uid pw]
    (when-let [hash (get-hash-for-uid (:password-store this) uid)]
      (verify-password pw hash)
      ;; TODO There is a slight security concern here. If no uid is
      ;; found, then the system will return slightly faster and this can
      ;; be measured by an attacker to discover usernames. I don't know
      ;; what the current advice is regarding this problem. I have
      ;; consdiered priming the user store with a 'nobody' password to
      ;; use.
      ))

  NewUserCreator
  (add-user! [this uid pw]
    (store-user-hash! (:password-store this) uid (create-hash (:rng this) pw))))

(defn new-user-domain []
  (component/using (->UserDomain) [:password-store]))

;; -------

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
                            :http-request-authorizer authorizer}))))))
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
                         (make-authorization-wrapper
                          (:http-request-authorizer opts)
                          (:failed-authorization-handler opts))))
        r)))
  (unresolve-handler [this m]
    (unresolve-handler routes m)))

(defn add-bidi-protection-wrapper [routes & {:as opts}]
  ["" (->ProtectMatched
       [routes]
       (s/validate
        {:http-request-authorizer (s/protocol HttpRequestAuthorizer)
         (s/optional-key :failed-authorization-handler) (s/protocol FailedAuthorizationHandler)}
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

(defn new-http-basic-request-authorizer [& {:as opts}]
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

(defn new-login-post-handler [handlers-p get-handler-key {:keys [user-password-authorizer http-session-store] :as opts}]
  (s/validate {:user-password-authorizer (s/protocol UserPasswordAuthorizer)
               :http-session-store (s/protocol HttpSessionStore)}
              opts)
  (fn [{{username "username" password "password" requested-uri "requested-uri"} :form-params
        routes :modular.bidi/routes}]

    (if (and username
             (not-empty username)
             (authorized-user? user-password-authorizer (.trim username) password))

      {:status 302
       :headers {"Location" requested-uri}
       :cookies (start-session! http-session-store username)}

      ;; Return back to login form
      {:status 302
       :headers {"Location" (path-for routes (get @handlers-p get-handler-key))}})))

(defn- make-login-handlers [opts]
  (let [p (promise)]
    @(deliver p {:get-handler (new-login-get-handler p :post-handler (select-keys opts [:boilerplate]))
                 :post-handler (wrap-params (new-login-post-handler p :get-handler (select-keys opts [:user-password-authorizer :http-session-store])))})))

(defrecord LoginForm [path context boilerplate]
  component/Lifecycle
  (start [this]
    (let [handlers (make-login-handlers (select-keys this [:user-password-authorizer :http-session-store :boilerplate]))]
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
     :http-request-authorizer (new-session-based-request-authorizer :http-session-store (:http-session-store this))
     :failed-authorization-handler (->BidiFailedAuthorizationRedirect (get-in this [:handlers :get-handler])))))

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
    (component/using (->LoginForm path context boilerplate) [:user-password-authorizer :http-session-store])))

;; Now we can build a protection domain, composed of a login form, user
;; authorizer and session store. Different constructors can build this
;; component in different ways.

(defrecord ProtectionDomain [protector user-password-authorizer http-session-store]
  component/Lifecycle
  (start [this] (component/start-system this (keys this)))
  (stop [this] (component/stop-system this (keys this)))
  ;; In this implementation, we export any routes provided by
  ;; sub-components. These are the routes that provide login forms and
  ;; so on, nothing to do with the routes that are protected by this
  ;; protection domain.
  BidiRoutesContributor
  (routes [this] ["" (vec (keep #(when (satisfies? BidiRoutesContributor %) (routes %)) (vals this)))])
  (context [this] (or
                   (first (keep #(when (satisfies? BidiRoutesContributor %) (context %))
                                ((juxt :protector :user-password-authorizer :http-session-store) this)))
                   ""))
  NewUserCreator
  (add-user! [_ uid pw]
    (if (satisfies? NewUserCreator user-password-authorizer)
      (add-user! user-password-authorizer uid pw)
      (throw (ex-info "This protection domain implementation does not support the creation of new users" {})))))

(def new-default-protection-domain-schema
  {:password-file s/Any
   (s/optional-key :session-timeout-in-seconds) s/Int
   (s/optional-key :boilerplate) (s/=> 1)
   })

(defn new-default-protection-domain [& {:as opts}]
  (s/validate new-default-protection-domain-schema opts)
  (map->ProtectionDomain
   {:protector (if-let [boilerplate (:boilerplate opts)]
                 (new-login-form :boilerplate boilerplate)
                 (new-login-form))
    :user-password-authorizer (component/using (new-user-domain) [:password-store])
    :password-store (new-password-file (:password-file opts))
    :http-session-store (new-atom-backed-session-store
                         (or (:session-timeout-in-seconds opts)
                             10))}))

;; Now that we have a protection domain, we want the ability to create
;; bidi routes components that can be protected by simply declaring a dependency upon the protection domain component.

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

(defn new-protected-bidi-routes
  "Create a set of protected routes. Routes can a bidi route structure, or
  a function that takes the component and returns a bidi route
  structure."
  [routes & {:as opts}]
  (let [{:keys [context]}
        (->> (merge {:context ""} opts)
             (s/validate {:context s/Str}))]
    (->ProtectedBidiRoutes routes context)))

(defn new-mandatory-protected-bidi-routes
  "Like new-protected-bidi-routes above, but the absence of
  a :protection-domain dependency will cause an error."
  [routes & {:as opts}]
  (component/using
   (apply new-protected-bidi-routes routes (apply concat (seq opts)))
   [:protection-domain]))
