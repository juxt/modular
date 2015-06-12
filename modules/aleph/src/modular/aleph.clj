;; Copyright © 2014 JUXT LTD.

(ns modular.aleph
  (:require
   [com.stuartsierra.component :refer (Lifecycle)]
   [schema.core :as s]
   [modular.ring :refer (request-handler WebRequestHandler)]
   [aleph.http :as http]
   ))

(defn get-handler
  "Extract the Ring handler from the component"
  [component]
  (or
   ;; Handlers can be specified as a constructor arg
   (request-handler (:handler component))

   ;; Or as a dependency (which satisfies WebRequestHandler)
   (when-let [provider (first (filter #(satisfies? WebRequestHandler %) (vals component)))]
     (request-handler provider))

   ;; Or an exception is thrown
   (throw (ex-info (format "aleph http server requires a handler, or a dependency that satisfies %s" WebRequestHandler) {}))))

(defrecord Webserver [handler]
  Lifecycle
  (start [component]
    (let [server (http/start-server (get-handler component) component)]
      (assoc component :server server)))
  (stop [component]
    (when-let [server (:server component)]
      (.close server)
      (dissoc component :server))))

(def new-webserver-schema
  {:port s/Int
   s/Keyword s/Any})

(defn new-webserver [& {:as opts}]
  (->> opts
    (merge {:port 0})
    (s/validate new-webserver-schema)
    map->Webserver))
