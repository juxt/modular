;; Copyright © 2014 JUXT LTD.

(ns modular.bidi
  (:require
   [schema.core :as s]
   [modular.ring :refer (WebRequestHandler WebRequestBinding)]
   [com.stuartsierra.component :as component]
   [bidi.bidi :as bidi :refer (match-route path-for)]
   [clojure.tools.logging :refer :all]
   [plumbing.core :refer (?>)]))

;; I've thought hard about a less enterprisy name for this protocol, but
;; components that satisfy it fit most definitions of web
;; services. There's an interface (URIs), coupled to an implementation
;; (via handlers)
(defprotocol WebService
  ;; Return a map, keys (usually namespaced) to Ring handler functions
  (request-handlers [_])
  ;; Return a bidi route structure, from patterns to keys in the above
  ;; request-handlers map. Do NOT use any wrappers such as
  ;; ->WrapMiddleware that assume the matches are functions (because
  ;; they won't be)
  (routes [_])
  ;; The 'moount' point in the URI tree.
  (uri-context [_]))

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

;; Production of a Ring handler from a single WebService component

(defrecord KeywordToHandler [matched handlers]
  bidi/Matched
  (resolve-handler [this m]
    (when-let [{:keys [handler] :as res} (bidi/resolve-handler matched m)]
      (if (keyword? handler)
        (assoc res :handler (get handlers handler))
        res)))

  (unresolve-handler [this m]
    (bidi/unresolve-handler matched m)))

(defn- wrap-info [h m]
  (fn [req] (h (merge m req))))

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
    (wrap-info
     (bidi/make-handler joined-routes)
     {::routes joined-routes
      ::handlers handlers})))

;; Production of a Ring handler from multiple WebService components

;; The ComponentPreference record modifies a bidi route structure to
;; preference a given component when forming a URI from a
;; keyword. Without ComponentPreference, components using identical
;; keywords in their request-handlers map could inadvertantly get in the
;; way of path-for calls from another component.

(defrecord ComponentPreference [matched component]
  bidi/Matched
  (resolve-handler [this m]
    (bidi/resolve-handler matched m))
  (unresolve-handler [this m]
    (if (keyword? (:handler m))
      (or
       ;; In case there's another component using the same key in a handler-map,
       ;; preference a path to an 'internal' handler first.
       (bidi/unresolve-handler matched (assoc m :handler [component (:handler m)]))
       (bidi/unresolve-handler matched m))
      (bidi/unresolve-handler matched m))))

(defn wrap-component-preference
  "Augment the routes entry bound to a request with data indicating the
  component owner of the handler. This information is used to modify the
  behaviour of the path-for function on those routes, such that the
  component owner is preferenced in the case where multiple handlers
  use identical keywords."
  [h component]
  (if (fn? h)
    (fn [req]
      (h (update-in req [::routes] (fn [r] ["" (->ComponentPreference [r] component)]))))
    h))

;; We use ComponentAddressable to allow the formation of URIs using
;; korks in addition to direct reference to handlers, which may be
;; difficult to obtain in modular applications.

;; If a keyword is used, e.g. (bidi/path-for routes :foo) then we try
;; the request-handlers of the calling component first, and if there is
;; no handler entry for that keyword, all other components'
;; request-handler maps are tried (in an undefined order). This is what
;; is meant by a component 'preference'.

;; If a keyword sequence is used, e.g. (bidi/path-for routes [:foo
;; :bar]) then :foo is interpreted as a dependency of the router
;; (usually a router is shared between multiple components) and :bar
;; is a handler in the request-handlers of :foo.

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
        (assoc res :handler (-> (get-in handlers [ckey handler])
                                (wrap-component-preference ckey)
                                (wrap-capture-component-on-error :component ckey :handler handler)))
        res)))

  (unresolve-handler [this m]
    (cond (coll? (:handler m))
          (when (= ckey (first (:handler m)))
            (bidi/unresolve-handler matched (update-in m [:handler] second)))
          :otherwise (bidi/unresolve-handler matched m))))

(defmacro logf-result [level msg form]
  `(let [res# ~form]
     (logf ~level ~msg (pr-str res#))
     res#))

(defrecord Router []
  component/Lifecycle
  (start [this]
    (let [handlers
          ;; Handlers is a two-level map from dependency key to the
          ;; dependency's handler map.
          (logf-result
           :debug "Bidi router determines handlers as %s"
           (apply merge
                  (for [[k v] this
                        :when (satisfies? WebService v)]
                    (try
                      {k (request-handlers v)}
                      (catch Throwable e (throw (ex-info "Failed to call request-handlers" {:k k :v v} e)))))))]

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
