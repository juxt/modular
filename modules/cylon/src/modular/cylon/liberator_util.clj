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

(ns modular.cylon.liberator-util
  (:require
   [modular.cylon :refer (new-composite-disjunctive-request-authorizer
                         new-session-based-request-authorizer
                         new-http-basic-request-authorizer
                         HttpSessionStore
                         UserPasswordAuthorizer
                         authorized-request?)]
   [schema.core :as s]))

;; REST

;; For a REST API, it is useful to support both HTTP Basic
;; Authentication (for machines) but to honor cookies passed from a
;; browser in an AJAX call, when the user has logged in via a login
;; form.

;; Here are some utility functions that take a protection domain and
;; return a function which takes a Ring request and returns whether that
;; request is authorized. This is useful to implement the authorizd? decision point in Liberator.


(defn make-composite-authorizer
  "Construct a composite authorizer, based on "
  [protection-domain]

  (let [{:keys [http-session-store user-password-authorizer]}
        (s/validate {:http-session-store (s/protocol HttpSessionStore)
                     :user-password-authorizer (s/protocol UserPasswordAuthorizer)
                     }
                    (select-keys protection-domain [:http-session-store :user-password-authorizer]))]
    (fn [context]
      (let [authorizer
            (new-composite-disjunctive-request-authorizer
             (new-session-based-request-authorizer :http-session-store http-session-store)
             (new-http-basic-request-authorizer :user-password-authorizer user-password-authorizer))]
        (authorized-request? authorizer (:request context))))))
