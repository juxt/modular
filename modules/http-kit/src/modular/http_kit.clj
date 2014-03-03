(ns modular.http-kit
  (:require
   [com.stuartsierra.component :as component]
   [clojure.tools.logging :refer :all]
   [org.httpkit.server :refer (run-server)]))

(defprotocol RingHandlerProvider
  (handler [_]))

(def default-port 8000)

(defrecord Webserver [port]
  component/Lifecycle
  (start [this]
    (let [h (handler (:ring-handler-provider this))]
      (assert handler)
      (if port
        (infof "port is %d" port)
        (warnf "port is nil, using default of %d" default-port))
      (assoc this :server (run-server h {:port (or port default-port)}))))

  (stop [this]
    (when-let [server (:server this)]
      (server)
      (dissoc this :server))))
