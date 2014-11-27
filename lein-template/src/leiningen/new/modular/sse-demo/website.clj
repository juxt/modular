(ns {{name}}.website
  (:require
   [clojure.core.async :refer (go >! <! buffer dropping-buffer sliding-buffer chan take!)]
   [clojure.pprint :refer (pprint)]
   [clojure.tools.logging :refer :all]
   [bidi.bidi :refer (path-for)]
   [bidi.ring :refer (redirect)]
   [com.stuartsierra.component :refer (Lifecycle using)]
   [hiccup.core :as hiccup]
   [modular.bidi :refer (WebService)]
   [ring.util.response :refer (response redirect-after-post)]))

(defn index [req]
  {:status 200
   :body (hiccup/html
          [:body
           [:h1 "HTTP Async"]
           [:p [:a {:href "/system.html"} "System"]]
           [:p [:a {:href "/channel.html"} "Channel"]]
           ])})

(defn show-system []
  (fn [req]
    {:status 200
     :body (hiccup/html
            [:body
             [:h2 "System"]
             [:pre (hiccup/h (with-out-str (pprint @(find-var 'dev/system))))]])}))

(defn show-channel [ch]
  (fn [req]
    {:status 200
     :body (hiccup/html
            [:body
             [:h2 "Channel"]
             [:p "Channel type: " (type ch)]
             [:p "Buffer type: " (.-buf ch)]
             [:p "Channel count: " (count (.-buf ch))]
             [:p "Channel value: " (pr-str (.-buf (.-buf ch)))]
             [:form {:action "/drop" :method :post}
            [:input {:type :submit :value "Drop"}]]])}))

(defn drop-from-channel [ch]
  (fn [req]
    (go (<! ch))
    (redirect-after-post "/channel.html")))

;; Components are defined using defrecord.

(defrecord Website []
  Lifecycle
  (start [component]
    (let [ch (chan (buffer 10))]
      ;; Let's load the channel up with some random data
      (go (dotimes [n 26] (>! ch (char (+ (int \A) n)))))
      (assoc component :channel ch)))
  (stop [component] component)

  ; modular.bidi provides a router which dispatches to routes provided
  ; by components that satisfy its WebService protocol
  WebService
  (request-handlers [component]
    ;; Return a map between some keywords and their associated Ring
    ;; handlers
    {::index index
     ::show-system (show-system)
     ::show-channel (show-channel (:channel component))
     ::drop (drop-from-channel (:channel component) )})

  ;; Return a bidi route structure, mapping routes to keywords defined
  ;; above. This additional level of indirection means we can generate
  ;; hyperlinks from known keywords.
  (routes [_] ["/" {"index.html" ::index
                    "" (redirect ::index)
                    "system.html" ::show-system
                    "channel.html" ::show-channel
                    "drop" ::drop}])

  ;; A WebService can be 'mounted' underneath a common uri context
  (uri-context [_] ""))

;; While not mandatory, it is common to use a function to construct an
;; instance of the component. This affords the opportunity to control
;; the construction with parameters, provide defaults and declare
;; dependency relationships with other components.

(defn new-website []
  (-> (map->Website {})))
