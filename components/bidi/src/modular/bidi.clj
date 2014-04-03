;; Copyright © 2014 JUXT LTD.

(ns modular.bidi
  (:require
   [modular.protocols :refer (Index)]
   [modular.core :as mod]
   [schema.core :as s]
   [modular.ring :refer (RingHandlerProvider)]
   [com.stuartsierra.component :as component]
   [bidi.bidi :as bidi]))

(defn deref-if-possible [x]
  (if (instance? clojure.lang.IDeref x)
    (if-not (realized? x)
      (throw (ex-info (format "Cannot deref %s as it is not yet realized." x) {:object x}))
      (deref x))
    x))

;; If necessary, routes and context can return deref'ables if necessary,
;; for example, if their values are not known until the component is
;; started.
(defprotocol BidiRoutesContributor
  (routes [_])
  (context [_]))

(defrecord BidiRoutes [routes context]
  component/Lifecycle
  (start [this]
    (let [routes (cond-> routes
                         (fn? routes) (apply [this]))]
      (assoc this :routes routes)))
  (stop [this] this)

  BidiRoutesContributor
  (routes [this] (:routes this))
  (context [this] context))

(def bidi-routes-options-schema {:context s/Str})

(defn new-bidi-routes
  "Create a component with a given set of bidi routes. An optional web
  context can be given as a keyword argument using the :context key. The
  routes can be a bidi route structure, or a function that returns
  one. When using a function, the component is passed as a single
  argument."
  [routes & {:as opts}]
  (let [{:keys [context]}
        (->> (merge {:context ""} opts)
             (s/validate bidi-routes-options-schema))]
    (new BidiRoutes routes context)))

(defn wrap-routes
  "Add the final set of routes from which the Ring handler is built."
  [h routes]
  (fn [req]
    (h (assoc req ::routes routes))))

(defrecord BidiRingHandlerProvider []
  component/Lifecycle
  (start [this]
    (assoc this :routes ["" (vec (for [v (vals this)
                                       :when (satisfies? BidiRoutesContributor v)]
                                   [(or (context v) "") [(routes v)]]))]))
  (stop [this] this)

  Index
  (types [this] #{BidiRoutesContributor})

  RingHandlerProvider
  (handler [this]
    (let [routes (:routes this)]
      (-> routes bidi/make-handler
       (wrap-routes routes)))))

;; Keep this around for future integration with Prismatic Schema
(defn new-bidi-ring-handler-provider []
  (new BidiRingHandlerProvider))
