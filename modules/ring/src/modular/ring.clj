;; Copyright © 2014 JUXT LTD.

(ns modular.ring
  (:require
   [com.stuartsierra.component :as component]
   [clojure.tools.logging :refer (debugf infof)]
   [schema.core :as s]))

(defprotocol WebRequestHandler
  "A component can satisfy WebRequestHandler if it can respond to a Ring request"
  (request-handler [_]
    "Provide a Ring request handler function, a function that takes the http
    request map as an argument and returns the response"))

(extend-protocol WebRequestHandler
  clojure.lang.AFunction
  (^{:doc "An 1-arity function is considered a WebRequestHandler satisfying
           component"}
   request-handler [this] this))

(defprotocol WebRequestBinding
  "Component satisfying WebRequestBinding can bind values into the request
   object"
  (request-binding [_]
    "Return a map that will be merged (with merge) into the request
    object"))

(defprotocol WebRequestMiddleware
  (request-middleware [_]
    "Return a function that takes a request handler and returns a request
     handler, usually a wrapper that delegates to the given Ring
     handler"))

;; A 1-arity function will be considered a WebRequestMiddleware
;; satisfying component
(extend-protocol WebRequestMiddleware
  clojure.lang.AFunction
  (request-middleware [this] this))

(defrecord WebRequestHandlerHead []
  WebRequestHandler
  (request-handler [this]
    (let [dlg (request-handler (:request-handler this))
          middleware (->> (vals this)
                          (filter (partial satisfies? WebRequestMiddleware))
                          (map request-middleware)
                          (apply comp))]
      (middleware
       (fn [req]
         (let [bindings
               (apply merge-with merge
                      (map #(request-binding %)
                           (filter (partial satisfies? WebRequestBinding) (vals this))))]
           (debugf "Request bindings are %s" (keys bindings))
           (dlg (merge req bindings))))))))

(defn new-web-request-handler-head
  "Returns a record satisfying WebRequestHandler request handler component adapts a request-handler with the various middleware and request bindings it depends on."
  [& {:as opts}]
  (component/using
   (->> opts
        (merge {})
        (s/validate {})
        map->WebRequestHandlerHead)
   [:request-handler]))
