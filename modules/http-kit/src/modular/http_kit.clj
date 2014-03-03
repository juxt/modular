(ns modular.http-kit
  (:require
   [com.stuartsierra.component :as component]
   [clojure.tools.logging :refer :all]
   [org.httpkit.server :refer (run-server)]))

(defprotocol RingHandlerProvider
  (handler [_]))

(defrecord Webserver [port]
  component/Lifecycle
  (start [this]
    (if-let [handler (some (comp :jig.ring/handler system) (:jig/dependencies config))]
      (let [server (run-server handler {:port (:port config)})]
        (assoc-in system [(:jig/id config) :server] server))
      system))
  (stop [this]
    (when-let [server (get-in system [(:jig/id config) :server])]
      ;; Stop the server by calling the function
      (infof "Stopping http-kit server")
      (server))
    (dissoc system (:jig/id config))))
