(ns modular.bidi
  (:require
   [com.stuartsierra.component :as component]
   [bidi.bidi :as bidi]))

(defprotocol RoutesContributor
  (routes [_])
  (context [_]))

(defrecord BidiRoutes [routes context]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)
  modular.bidi/RoutesContributor
  (routes [this] routes)
  (context [this] context))

;; Keep this around for integration with Prismatic Schema
(defn new-bidi-routes [routes context]
  (new BidiRoutes routes context))

(defn wrap-routes
  "Add the final set of routes from which the Ring handler is built."
  [h routes]
  (fn [req]
    (h (assoc req :routes routes))))

(defrecord BidiRingHandlerProvider []
  component/Lifecycle
  (start [this] this)
  (stop [this] this)
  modular.http-kit/RingHandlerProvider
  (handler [this]
    (assert (:routes-contributors this) "No :routes-contributors found")
    (let [routes ["" (mapv #(vector (or (modular.bidi/context %) "") [(modular.bidi/routes %)])
                           (:routes-contributors this))]]
      (-> routes
          bidi/make-handler
          (wrap-routes routes)))))

;; Keep this around for integration with Prismatic Schema
(defn new-bidi-ring-handler-provider []
  (new BidiRingHandlerProvider))
