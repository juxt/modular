;; Copyright © 2014, JUXT LTD. All Rights Reserved.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

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
    (println "On session, this is " this)
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
