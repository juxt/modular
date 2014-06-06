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
  (ring-binding [_]
    "Return a map that will be merged (with merge) into the request
    object"))

(defprotocol RingMiddleware
  "An ordinary single-arity function will suffice as a RingMiddleware
   satisfying component"
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
          middleware (->> (vals this)
                          (filter (partial satisfies? RingMiddleware))
                          (apply comp))]
      (middleware
       (fn [req]
         (let [bindings
               (apply merge-with merge
                      (map #(ring-binding %)
                           (filter (partial satisfies? RingBinding) (vals this))))]
           (debugf "Request bindings are %s" (keys bindings))
           (dlg (merge req bindings))))))))

(defn new-ring-head
  "A ring head component adapts a ring-handler with the various
  middleware and request bindings it depends on."
  []
  (component/using (->RingHead) [:ring-handler]))
