(ns {{name}}.hello
  (:require
   [com.stuartsierra.component :refer (Lifecycle using)]
   [aleph.http :as http]
   [aleph.netty :refer (AlephServer)]
   [schema.core :as s]
   [yada.yada :refer (yada)]))

(defn get-handler []
  (yada "Hello World!")
  )

(s/defrecord HelloWorld [port :- s/Int
                         server :- (s/protocol AlephServer)]
  Lifecycle
  (start [component]
         (let [server (http/start-server (get-handler) component)]
           (assoc component
                  :server server
                  :port (if (pos? port) port (aleph.netty/port server)))))
  (stop [component]
        (when-let [server (:server component)] (.close server))
        (dissoc component :server)))

(defn new-hello-world [& {:as opts}]
  (map->HelloWorld opts))
