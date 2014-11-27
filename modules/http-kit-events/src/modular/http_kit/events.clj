;; Copyright © 2014 JUXT LTD.

(ns modular.http-kit.events
  (:require
   [clojure.core.async :as async :refer (go <! go-loop)]
   [com.stuartsierra.component :as component]
   [modular.async :refer (channel)]
   [modular.bidi :refer (WebService)]
   [org.httpkit.server :refer (with-channel send! on-close)]
   [schema.core :as s]))

(def headers {"Content-Type" "text/event-stream"})

(defn ->message [data]
  (str "data: " data "\r\n\r\n"))

(defn server-event-source [ch]
  ;; Adding a mult here is dangerous because it bleeds the underlying
  ;; channel dry. We should provide the mult via modular.async
  (let [m (async/mult ch)]
    (fn [req]
      (let [ch (async/chan 16)]
        (async/tap m ch)
        (with-channel req net-ch
          (on-close net-ch (fn [_]
                             (async/untap m ch)
                             (async/close! ch)))
          (send! net-ch {:headers headers} false)
          (go-loop []
            (when-let [data (<! ch)]
              (println "Got data! " data)
              (send! net-ch (->message data) false)
              (recur))))))))

(defrecord EventService [channel-provider]
  WebService
  (request-handlers [this] {::events (server-event-source (channel channel-provider))})
  (routes [_] ["" ::events])
  (uri-context [_] "/events"))

(defn new-event-service [& {:as opts}]
  (component/using
   (->> opts
        (merge {})
        map->EventService)
   [:channel-provider]))
