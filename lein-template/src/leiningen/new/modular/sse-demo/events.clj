(ns {{name}}.events
  (:require
   [clojure.core.async :as async :refer (go go-loop >! <! buffer dropping-buffer sliding-buffer chan put! mult tap)]
   [clojure.java.io :as io]
   [clojure.pprint :refer (pprint)]
   [clojure.tools.logging :refer :all]
   [bidi.bidi :refer (path-for RouteProvider handler)]
   [bidi.ring :refer (redirect)]
   [com.stuartsierra.component :refer (Lifecycle using)]
   [hiccup.core :as hiccup]
   [org.httpkit.server :as httpkit]
   [ring.util.response :refer (response redirect-after-post)]
   [ring.middleware.params :refer (params-request)]))

(defn index [req]
  {:status 200
   :body (hiccup/html
          [:body
           [:h1 "HTTP Async"]
           [:p [:a {:href "system.html"} "System"]]
           [:p [:a {:href "chat.html"} "Chat"]]
           ])})

(defn show-system []
  (fn [req]
    {:status 200
     :body (hiccup/html
            [:body
             [:h2 "System"]
             [:pre (hiccup/h (with-out-str (pprint @(find-var 'dev/system))))]])}))

(defn show-chat [{:keys [messages]}]
  (fn [req]
    {:status 200
     :body (hiccup/html
            [:html
             [:head
              [:title "Chat"]
              [:script {:src "/jquery/jquery.min.js"}]]
             [:body
              [:h2 "Chat"]
              [:ul#messages
               (for [msg @messages]
                 [:li msg])]
              [:div
               [:input#message {:type :text :name :message}]
               [:button#chat "Chat"]]
              [:script (slurp (io/resource "events.js"))]]])}))

(defn post-to-channel [{:keys [messages channel]}]
  (fn [req]
    (let [msg (-> req params-request :form-params (get "message"))]
      (swap! messages conj msg)
      (put! channel msg))
    {:status 200 :body "Thank you"}))

(defn server-event-source
  "Take a mult and return a handler that will produce HTML5 server-sent
  event (SSE) streams"
  [mlt]
  (fn [req]
    (let [ch (chan 16)]
      (tap mlt ch)
      (httpkit/with-channel req net-ch
        (httpkit/on-close net-ch (fn [_]
                                   (async/untap mlt ch)
                                   (async/close! ch)))
        (httpkit/send! net-ch {:headers {"Content-Type" "text/event-stream"}} false)
        (go-loop []
          (when-let [data (<! ch)]
            (httpkit/send! net-ch (str "data: " data "\r\n\r\n") false)
            (recur)))))))

;; Components are defined using defrecord.

(defrecord Website []
  Lifecycle
  (start [component]
    (let [ch (chan (buffer 10))]
      ;; Let's create a mult
      (assoc component
             :channel ch
             :messages (atom [])
             :mult (mult ch))))
  (stop [component] component)

  RouteProvider
  ;; Return a bidi route structure, mapping routes to keywords defined
  ;; above. This additional level of indirection means we can generate
  ;; hyperlinks from known keywords.
  (routes [component]
    ["/events/"
     {"index.html" (handler ::index index)
      "" (redirect ::index)
      "system.html" (handler ::show-system (show-system))
      "chat.html" (handler ::show-chat (show-chat component))
      "messages" (handler ::post (post-to-channel component))
      "events" (handler ::events (server-event-source (:mult component)))}]))

;; While not mandatory, it is common to use a function to construct an
;; instance of the component. This affords the opportunity to control
;; the construction with parameters, provide defaults and declare
;; dependency relationships with other components.

(defn new-events-website []
  (-> (map->Website {})))
