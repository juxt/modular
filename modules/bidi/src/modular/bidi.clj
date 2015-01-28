;; Copyright © 2014 JUXT LTD.

(ns modular.bidi
  (:require
   [schema.core :as s]
   [modular.ring :refer (WebRequestHandler)]
   [com.stuartsierra.component :as component :refer (Lifecycle)]
   [bidi.bidi :as bidi :refer (match-route resolve-handler)]
   [bidi.ring :refer (resources-maybe make-handler redirect)]
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
  Lifecycle
  (start [component]
    (assoc component
           :target (resources-maybe
                    {:prefix resource-prefix
                     ;; By adding uri-context, we ensure that the
                     ;; path-for function works properly, as it uses an
                     ;; = check, so we avoid the situation where the
                     ;; wrong instance of this record returns a non-nil
                     ;; value
                     ::uri-context uri-context})))
  (stop [component] component)
  WebService
  (request-handlers [_] {})
  (routes [component]
    ["" (:target component)])
  (uri-context [_] uri-context))

(def new-static-resource-service-schema
  {:uri-context s/Str
   :resource-prefix s/Str})

(defn new-static-resource-service [& {:as opts}]
  (->> opts
    (merge {:uri-context "/"})
    (s/validate new-static-resource-service-schema)
    (map->StaticResourceService)))

(defrecord Redirect [from to]
  WebService
  (request-handlers [_] {})
  (routes [component]
    [from (redirect to)])
  (uri-context [_] ""))

(defn new-redirect [& {:as opts}]
  (->> opts
    (merge {:from "/"})
    (s/validate {:from s/Str :to (s/either s/Keyword s/Str)})
    map->Redirect))

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

;; TODO Support bidi route compilation
(defn as-request-handler
  "Take a WebService component and return a Ring handler."
  [service not-found-handler]
  (assert (satisfies? WebService service))
  (some-fn
   (make-handler
    [(or (uri-context service) "")
     (->KeywordToHandler [(routes service)]
                         (request-handlers service))])
   not-found-handler))

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

;; Replace keywords with their actual handlers. This is done by wrapping
;; the handler and providing a proxy resolve-handler method which looks
;; up the actual handler using the keyword and the handlers map. The
;; keyword can then be used as the target in a call to
;; bidi.bidi/path-for, rather than the handler itself.
(defrecord KeywordIndirection [matched ckey handlers add-exception-context?]
  bidi/Matched
  (resolve-handler [this m]
    (when-let [{:keys [handler] :as res} (bidi/resolve-handler matched m)]
      (if
          ;; Special handler type, actual Ring handler is indirectly
          ;; resolvable through the handlers map.
          (keyword? handler)
        (assoc res
               ::component ckey
               :handler (cond-> (get handlers handler)
                          ;; This should be based on given settings
                          add-exception-context? (wrap-capture-component-on-error :component ckey :handler handler)))
        ;; Otherwise continue to return the original result
        res)))

  (unresolve-handler [this m]
    (bidi/unresolve-handler matched m)))

(defrecord Router [not-found-handler add-exception-context?]
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
                            (->KeywordIndirection [(routes v)] ckey (get handlers ckey) add-exception-context?)]))])))
  (stop [this] this)

  WebService
  (request-handlers [this] (:handlers this))
  (routes [this] (:routes this))
  (uri-context [this] (:uri-context this))

  WebRequestHandler
  (request-handler [this] (as-request-handler this not-found-handler)))

(def new-router-schema
  {:uri-context s/Str
   :add-exception-context? s/Bool
   :not-found-handler (s/=>* {:status s/Int
                              s/Keyword s/Any}
                             [{:uri s/Str
                                s/Keyword s/Any}])})

(defn new-router
  "Constructor for a ring handler that collates all bidi routes
  provided by its dependencies."
  [& {:as opts}]
  (->> opts
    (merge {:uri-context ""
            :add-exception-context? false
            :not-found-handler (constantly {:status 404 :body "Not found"})})
       (s/validate new-router-schema)
       map->Router))

;; ------  TODO Router needs to display all possible routes available,
;; ------  as debug data, so that people can see easily which routes are
;; ------  available. This addresses one of the more difficult and
;; ------  potentially frustrating cases of "computer says no" when the
;; ------  URI doesn't seem to dispatch to anything and no clues as to
;; ------  why! These routes can be determined by a tree walk.
