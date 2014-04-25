;; Copyright © 2014 JUXT LTD.

(ns modular.ring
  (:require
   [com.stuartsierra.component :as component]
   [clojure.tools.logging :refer (infof)]))

(defprotocol RingHandler
  (ring-handler [_]))

(defprotocol RingBinding
  (ring-binding [_ req]))

(defrecord RingBinder []
  component/Lifecycle
  (start [this] (assoc this
                  ::bindings (filter (partial satisfies? RingBinding) (vals this))))
  (stop [this] this)
  RingHandler
  (ring-handler [this]
    (let [dlg (ring-handler (:ring-handler this))]
      (fn [req]
        (let [bindings
              (apply merge (map #(ring-binding % req) (::bindings this)))]
          (infof "Request bindings are %s" (keys bindings))
          (dlg (merge req bindings)))))))

(defn new-ring-binder []
  (component/using (->RingBinder) [:ring-handler]))
