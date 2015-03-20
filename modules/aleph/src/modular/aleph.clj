;; Copyright © 2014 JUXT LTD.

(ns modular.aleph
  (:require
   [com.stuartsierra.component :refer (Lifecycle)]
   [schema.core :as s]
   [modular.ring :refer (request-handler WebRequestHandler)]
   [aleph.http :as http]))

(defrecord Webserver [port]
  Lifecycle
  (start [component]
    (if-let [provider (first (filter #(satisfies? WebRequestHandler %) (vals component)))]
      (let [h (request-handler provider)]
        (assert h)
        (let [server (http/start-server h {:port port})]
          (assoc component :server server)))
      (throw (ex-info (format "aleph http server requires the existence of a component that satisfies %s" WebRequestHandler)
                      {:component component})))

    )
  (stop [component]
    (when-let [server (:server component)]
      (.close server)
      (dissoc component :server))))

(def new-webserver-schema {:port s/Int})

(defn new-webserver [& {:as opts}]
  (->> opts
    (merge {:port 8080})
    (s/validate new-webserver-schema)
    map->HttpServer))
