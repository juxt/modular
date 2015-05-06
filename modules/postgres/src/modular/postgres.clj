(ns modular.postgres
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [schema.core :as s])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn- pool
  [{:keys [url user password max-connections] :or {max-connections 30}}]
  (doto (ComboPooledDataSource.)
    (.setDriverClass "org.postgresql.Driver")
    (.setJdbcUrl url)
    (.setUser user)
    (.setPassword password)

    (.setMinPoolSize 1)
    (.setMaxPoolSize max-connections)
    (.setAcquireIncrement 1)
    (.setPreferredTestQuery "select 1")

    (.setTestConnectionOnCheckout true)
    (.setCheckoutTimeout 7000)
    (.setAcquireRetryAttempts 3)
    (.setAcquireRetryDelay 1000)

    ;; expire excess connections after 30 minutes of inactivity:
    (.setMaxIdleTimeExcessConnections (* 30 60))
    ;; expire connections after 3 hours of inactivity:
    (.setMaxIdleTime (* 3 60 60))))

(defrecord Postgres [url user password]
  component/Lifecycle
  (start [this]
    (log/debug "Starting postgres")
    (assoc this :pool (pool this)))

  (stop [{:keys [pool] :as this}]
    (when pool
      (log/debug "Stopping postgres")
      (.close pool)
      (log/debug "Pool closed"))
    (dissoc this :pool)))

(defn new-postgres-connection-pool [& {:as opts}]
  (->> opts
       (merge {})
       (s/validate {:url s/Str
                    :user s/Str
                    :password s/Str})
       map->Postgres))
