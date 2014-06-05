(ns {{name}}.website
  (:require
   [modular.ring :refer (RingHandler)]
   [modular.bidi :refer (WebService as-ring-handler)]
   [hiccup.core :refer (html)]
   [liberator.core :refer (resource)]
   [bidi.bidi :refer (path-for)]))

{{! "We change the delimiters to avoid conflicting with nested destructuring in the code below" }}
{{=<% %>=}}
(defn index []
  {:available-media-types #{"text/html"}
   :handle-ok (fn [{{routes :modular.bidi/routes} :request}]
                (html
                 [:head
                  [:title "Welcome"]]
                 [:body
                  [:h1 "Hello World! from <% name %>"]
                  [:h2 "Links"]
                  [:p [:a {:href (path-for routes ::main)} "Home"]]
                  ]))})

(defrecord Website []
  ;; Components such as this provide routes and handlers under an
  ;; optional uri prefix (called the uri-context)
  WebService
  (ring-handler-map [_]
    {::main (-> (index) resource)})

  (routes [_] ["/" ::main])

  (uri-context [_] "")

  ;; By satisfying the RingHandler protocol, this component can be made
  ;; a direct dependency of a Ring-compatible web server like http-kit
  ;; or Jetty. Whether this is the only web-serving component in an
  ;; application, or one of many (composed together by a router),
  ;; depends on the system's dependency map defined in system.clj
  RingHandler
  (ring-handler [this] (as-ring-handler this)))

(defn new-website []
  (->Website))
