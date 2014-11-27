(ns {{name}}.website
  (:require
   [bidi.ring :refer (redirect)]
   [modular.bidi :refer (WebService)]
   [clojure.pprint :refer (pprint)]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer (using)]
   [hiccup.core :as hiccup]
   [ring.util.response :refer (response)]))

(defn index [req]
  {:status 200
   :body (hiccup/html [:h1 "HTTP Asynchronous Services Demo"])})

;; Components are defined using defrecord.

(defrecord Website []

  ; modular.bidi provides a router which dispatches to routes provided
  ; by components that satisfy its WebService protocol
  WebService
  (request-handlers [this]
    ;; Return a map between some keywords and their associated Ring
    ;; handlers
    {::index index})


  ;; Return a bidi route structure, mapping routes to keywords defined
  ;; above. This additional level of indirection means we can generate
  ;; hyperlinks from known keywords.
  (routes [_] ["/" {"index.html" ::index
                    "" (redirect ::index)}])

  ;; A WebService can be 'mounted' underneath a common uri context
  (uri-context [_] ""))

;; While not mandatory, it is common to use a function to construct an
;; instance of the component. This affords the opportunity to control
;; the construction with parameters, provide defaults and declare
;; dependency relationships with other components.

(defn new-website []
  (-> (map->Website {})
      (using [])))
