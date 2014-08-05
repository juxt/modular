;; Copyright © 2014 JUXT LTD.

(ns modular.ring
  (:require
   [com.stuartsierra.component :as component]
   [clojure.tools.logging :refer (debugf infof)]
   [schema.core :as s]
   [clojure.pprint :refer (pprint)]))

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
     handler."))

;; A 1-arity function will be considered a WebRequestMiddleware
;; satisfying component
(extend-protocol WebRequestMiddleware
  clojure.lang.AFunction
  (request-middleware [this] this))

;; A WebRequestHandlerHead component may be placed 'in front of' a
;; delegate WebRequestHandler. It allows middleware and bindings to be
;; added via explicit dependency wiring. The advantage of this approach,
;; in comparison to traditional Ring middleware chaining, is that this
;; opens middleware chains for extension. The 'open for extension'
;; principle (also exhibited by Clojure's multimethods and records) is
;; an important feature of a modular application and considered worth
;; the extra effort required in understanding it.

(defrecord WebRequestHandlerHead []
  WebRequestHandler
  (request-handler [this]
    (let [dlg (when-let [wrh (or (:request-handler this) ; explicit delegate
                                 ;; or implicitly discovered
                                 (->> this vals
                                      (filter (partial satisfies? WebRequestMiddleware))
                                      ;; choose a winner
                                      first))]
                (request-handler wrh))
          middleware (->> (vals this)
                          (filter (partial satisfies? WebRequestMiddleware))
                          (map request-middleware)
                          (apply comp))]
      (middleware
       (fn [req]
         (let [bindings
               (->> this vals
                    (filter (partial satisfies? WebRequestBinding))
                    (map #(request-binding %))
                    (apply merge-with merge))]
           (if dlg
             (dlg (merge req bindings))
             ;; A nicer (than nil) default :-
             {:status 200 :body (str "No request handler dependency, request is \n"
                                     (with-out-str (pprint (merge req bindings))))})))))))

(defn new-web-request-handler-head
  "Returns a record satisfying WebRequestHandler request handler component adapts a request-handler with the various middleware and request bindings it depends on."
  [& {:as opts}]
  (->> opts
       (merge {})
       (s/validate {})
       map->WebRequestHandlerHead))
