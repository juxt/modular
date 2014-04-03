;; Copyright © 2014 JUXT LTD.

(ns modular.cassandra
  (:require
   [schema.core :as s]
   [com.stuartsierra.component :as component]
   [clojurewerkz.cassaforte.client :as client]))

(defrecord Cluster [opts]
  component/Lifecycle
  (start [this]
    (assoc this :cluster (client/build-cluster opts)))
  (stop [this]
    (when-let [cluster (:cluster this)]
      (.shutdown cluster))
    this))

(def ClusterSchema
  {:contact-points [s/Str]
   :port s/Int})

(def ClusterDefaults
  {:contact-points ["127.0.0.1"]
   :port 9042})

(defn new-cluster [opts]
  (->Cluster (s/validate ClusterSchema (merge ClusterDefaults opts))))

(defrecord Session [opts]
  component/Lifecycle
  (start [this]
    (assoc this :session (client/connect (get-in this [:cluster :cluster]) (:keyspace opts))))
  (stop [this]
    (when-let [session (:session this)]
      (.shutdown session))
    this))

(def SessionSchema
  {:keyspace s/Keyword})

(def SessionDefaults {})

(defn new-session [opts]
  (->Session (s/validate SessionSchema (merge SessionDefaults opts))))
