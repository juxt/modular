;; Copyright © 2014 JUXT LTD.

(ns modular.bidi
  (:require
   [schema.core :as s]
   [modular.ring :refer (WebRequestHandler WebRequestBinding)]
   [com.stuartsierra.component :as component]
   [bidi.bidi :as bidi :refer (match-route resolve-handler)]
   [bidi.ring :refer (resources-maybe make-handler)]
   [clojure.tools.logging :refer :all]
   [plumbing.core :refer (?>)]))

;; I've thought hard about a less enterprisy name for this protocol, but
;; components that satisfy it fit most definitions of web
;; services. There's an interface (URIs), coupled to an implementation
;; (via handlers)
(defprotocol WebService
  (request-handlers [_]
    "Return a map, keys (usually namespaced) to Ring handler functions")
  (routes [_]
    "Return a bidi route structure, from patterns to keys in the above request-handlers map. Do NOT use any wrappers such as ->WrapMiddleware that assume the matches are functions (because they won't be)")
  (uri-context [_]
    "The 'mount' point in the URI tree."))

(defrecord WebServiceFromArguments [request-handlers routes uri-context]
  WebService
  (request-handlers [this] request-handlers)
  (routes [this] routes)
  (uri-context [this] uri-context))

(def new-web-service-schema
  {:request-handlers {s/Keyword s/Any}
   :routes [(s/one s/Any "pattern") (s/one s/Any "matched")]
   :uri-context s/Str})

(defn new-web-service
  "Create a component with a given set of bidi routes. An optional web
  context can be given as a keyword argument using the :context key. The
  routes can be a bidi route structure, or a function that returns
  one. When using a function, the component is passed as a single
  argument."
  [& {:as opts}]
  (->> opts
       (merge {:uri-context ""} opts)
       (s/validate new-web-service-schema)
       map->WebServiceFromArguments))

(defrecord StaticResourceService [uri-context resource-prefix]
  WebService
  (request-handlers [_] {})
  (routes [_]
    [uri-context (resources-maybe {:prefix resource-prefix})])
  (uri-context [_] ""))

(def new-static-resource-service-schema
  {:uri-context s/Str
   :resource-prefix s/Str})

(defn new-static-resource-service [& {:as opts}]
  (->> opts
       (merge {})
       (s/validate new-static-resource-service-schema)
       (map->StaticResourceService)))

;; Production of a Ring handler from a single WebService component

(defrecord KeywordToHandler [matched handlers]
  bidi/Matched
  (resolve-handler [this m]
    (when-let [{:keys [handler] :as res} (resolve-handler matched m)]
      (if (keyword? handler)
        (assoc res :handler (get handlers handler))
        res)))

  (unresolve-handler [this m]
    (bidi/unresolve-handler matched m)))

;; TODO Support route compilation
(defn as-request-handler
  "Take a WebService component and return a Ring handler."
  [service]
  (assert (satisfies? WebService service))
  (let [routes (routes service)
        handlers (request-handlers service)
        ;; Create a route structure which can dispatch to handlers but
        ;; still allow URI formation via keywords.
        joined-routes [(or (uri-context service) "")
                       (->KeywordToHandler [routes] handlers)]]
    (make-handler joined-routes)))

;; -----------------------------------------------------------------------

(defn wrap-capture-component-on-error
  "Wrap handler in a try/catch that will capture the component and
  handler of the error."
  [h & {:keys [component handler]}]
  (when h
    (fn [req]
      (try
        (h req)
        (catch Exception cause
          (throw (ex-info "Failure during request handling"
                          {:component component :handler handler}
                          cause)))))))

(defrecord ComponentAddressable [matched ckey handlers]
  bidi/Matched
  (resolve-handler [this m]
    (when-let [{:keys [handler] :as res} (bidi/resolve-handler matched m)]
      (if (keyword? handler)
        (assoc res
          :handler (cond-> (get-in handlers [ckey handler])
                           ;; This should be based on given settings
                           false (wrap-capture-component-on-error :component ckey :handler handler)))
        res)))

  (unresolve-handler [this m]
    (bidi/unresolve-handler matched m)))

(defrecord Router []
  component/Lifecycle
  (start [this]
    (let [handlers
          (apply merge
                 (for [[k v] this :when (satisfies? WebService v)]
                   (try
                     {k (request-handlers v)}
                     (catch Throwable e (throw (ex-info "Failed to call request-handlers" {:k k :v v} e))))))]

      (assoc this
        :handlers handlers
        :routes ["" (vec (for [[ckey v] this
                               :when (satisfies? WebService v)]
                           [(or (uri-context v) "")
                            ;; We wrap in some bidi middleware which
                            ;; allows us to form URIs via a
                            ;; keyword-path: [component-key handler-key]
                            (->ComponentAddressable [(routes v)] ckey handlers)]))])))
  (stop [this] this)

  WebService
  (request-handlers [this] (:handlers this))
  (routes [this] (:routes this))
  (uri-context [this] (:uri-context this))

  WebRequestHandler
  (request-handler [this] (as-request-handler this)))

(def new-router-schema
  {:uri-context s/Str})

(defn new-router
  "Constructor for a ring handler that collates all bidi routes
  provided by its dependencies."
  [& {:as opts}]
  (->> opts
       (merge {:uri-context ""})
       (s/validate new-router-schema)
       map->Router))

;; ------  TODO Router needs to display all possible routes available,
;; ------  as debug data, so that people can see easily which routes are
;; ------  available. This addresses one of the more difficult and
;; ------  potentially frustrating cases of "computer says no" when the
;; ------  URI doesn't seem to dispatch to anything and no clues as to
;; ------  why! These routes can be determined by a tree walk.
