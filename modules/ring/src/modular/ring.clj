;; Copyright © 2014 JUXT LTD.

(ns modular.ring
  (:require
   [com.stuartsierra.component :as component]
   [clojure.tools.logging :refer (debugf)]))

(defprotocol RingHandler
  "Component can satisfy RingHandler if they provide a whole web
   application"
  (ring-handler [_]
    "Provide a Ring handler function, a function that takes the http
    request map as an argument and returns the response"))

(extend-protocol RingHandler
  clojure.lang.AFunction
  (^{:doc "An ordinary function will suffice as a RingHandler satisfying
           component"}
   ring-handler [this] this))

(defprotocol RingBinding
  "Component satisfying RingBinding can bind values into the request
   object"
  (ring-binding [_ req]
    "Return a map that will be merged into the request object"))

(defprotocol RingMiddleware
  "An ordinary function will suffice as a RingMiddleware satisfying
   component"
  (ring-middleware [_]
    "Return a function that takes a Ring handler and returns a Ring
     handler, usually a wrapper that delegates to the given Ring
     handler"))

(extend-protocol RingMiddleware
  clojure.lang.AFunction
  (ring-middleware [this] this))

(defrecord RingHead []
  RingHandler
  (ring-handler [this]
    (let [dlg (ring-handler (:ring-handler this))
          middleware (apply comp (filter (partial satisfies? RingMiddleware) (vals this)))]
      (middleware
       (fn [req]
         (let [bindings
               (apply merge-with merge
                      (map #(ring-binding % req)
                           (filter (partial satisfies? RingBinding) (vals this))))]
           (debugf "Request bindings are %s" (keys bindings))
           (dlg (merge req bindings))))))))

(defn new-ring-head []
  (component/using (->RingHead) [:ring-handler]))
