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
   [bidi.bidi :as bidi :refer (path-for resolve-handler unresolve-handler)]
   [schema.core :as s]
   [ring.middleware.cookies :refer (wrap-cookies)]
   [com.stuartsierra.component :as component])
  (:import
   (javax.xml.bind DatatypeConverter)))

(defprotocol HttpRequestAuthorizer
  ;; Return the request, modified if necessary, if the request is authorized
  (authorized-request? [_ request]))

(extend-protocol HttpRequestAuthorizer
  Boolean
  (authorized-request? [this request] this))

(defprotocol UserPasswordAuthorizer
  (authorized-user? [_ user password]))

(extend-protocol UserPasswordAuthorizer
  Boolean
  (authorized-user? [this user password] this))

(defprotocol FailedAuthorizationHandler
  (failed-authorization [_ request]))

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

(defrecord Protect [routes opts]
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
  (->> opts
       (s/validate
        {:authorizer (s/protocol HttpRequestAuthorizer)
         (s/optional-key :fail-handler) (s/protocol FailedAuthorizationHandler)})
       (->Protect routes)))

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
