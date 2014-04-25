(ns modular.mqtt
  (:require
   [modular.netty :refer (NettyHandlerProvider)]
   [com.stuartsierra.component :as component]
   [mqtt.decoder :refer (make-decoder)]
   [mqtt.encoder :refer (make-encoder)]))

(defrecord MqttDecoder []
  component/Lifecycle
  (start [this]
    (assoc this :provider #(make-decoder)))
  (stop [this] this)
  NettyHandlerProvider
  (netty-handler [this] (:provider this))
  (priority [this] 10))

(defn new-mqtt-decoder
  []
  (->MqttDecoder))

(defrecord MqttEncoder []
  component/Lifecycle
  (start [this]
    (assoc this :provider #(make-encoder)))
  (stop [this] this)
  NettyHandlerProvider
  (netty-handler [this] (:provider this))
  (priority [this] 10))

(defn new-mqtt-encoder
  []
  (->MqttEncoder))
