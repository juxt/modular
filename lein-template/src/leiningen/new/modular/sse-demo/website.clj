(ns {{name}}.website
  (:require
   [clojure.core.async :refer (go >! <! buffer)]
   [clojure.pprint :refer (pprint)]
   [clojure.tools.logging :refer :all]
   [bidi.ring :refer (redirect)]
   [com.stuartsierra.component :refer (Lifecycle using)]
   [hiccup.core :as hiccup]
   [modular.bidi :refer (WebService)]
   [ring.util.response :refer (response)]))

(defn index [ch]
  (fn [req]
    {:status 200
     :body (hiccup/html
            [:h1 "HTTP Asynchronous Services Demo"]
            [:p "System: " [:pre (hiccup/h (with-out-str (pprint @(find-var 'dev/system))))]]
            [:p "Channel value: " (pr-str (.-buf (.-buf ch)))])}))

;; Components are defined using defrecord.

(defrecord Website [channel]
  Lifecycle
  (start [component]
    ;; Let's load the channel up with some random data
    (go (dotimes [_ 10] (>! (:channel channel) (rand-int 20))))
    component)
  (stop [component] component)

  ; modular.bidi provides a router which dispatches to routes provided
  ; by components that satisfy its WebService protocol
  WebService
  (request-handlers [component]
    ;; Return a map between some keywords and their associated Ring
    ;; handlers
    {::index (index (:channel channel))})


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
      (using [:channel])))
