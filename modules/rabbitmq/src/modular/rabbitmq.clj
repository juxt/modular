;; Copyright © 2014 JUXT LTD.

(ns modular.rabbitmq
  (:require [langohr.core    :as rmq]
            [langohr.channel :as lch]
            [com.stuartsierra.component :as component]))

;;
;; API
;;

(defrecord Client [opts]
  component/Lifecycle
  (start [this]
    (let [conn (rmq/connect opts)
          ch   (lch/open conn)]
      (assoc this :connection conn :channel    ch)))
  (stop [this]
    (let [conn (:connection this)
          ch   (:channel this)]
      (.abort ch)
      (.abort conn)
      this)))

(defn new-rabbitmq-client
  [opts]
  (Client. opts))
