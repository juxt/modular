;; Copyright © 2014 JUXT LTD.

(ns modular.http-kit
  (:require
   [schema.core :as s]
   [com.stuartsierra.component :as component]
   [clojure.tools.logging :refer :all]
   [modular.ring :refer (ring-handler)]
   [org.httpkit.server :refer (run-server)]))

(def default-port 3000)

(defrecord Webserver [port]
  component/Lifecycle
  (start [this]
    (if-let [provider (first (filter #(satisfies? modular.ring/RingHandler %) (vals this)))]
      (let [h (ring-handler provider)]
        (assert h)
        (let [server (run-server h {:port port})]
          (assoc this :server server)))
      (throw (ex-info (format "http-kit module requires the existence of a component that satisfies %s" modular.ring/RingHandler) {:this this}))))

  (stop [this]
    (when-let [server (:server this)]
      (server)
      (dissoc this :server))))

(defn new-webserver [& {:as opts}]
  (let [{:keys [port]} (->> (merge {:port default-port} opts)
                            (s/validate {:port s/Int}))]
    (->Webserver port)
    ))
