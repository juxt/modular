;; Copyright © 2014 JUXT LTD.

(ns modular.async
  "Make a core.async channel available as a component, so that
  channels can be named and wired up in the system configuration."
  (:require
   [com.stuartsierra.component :as component :refer (Lifecycle)]
   [clojure.core.async :refer (chan mult tap)]
   [clojure.core.async.impl.protocols :as aimpl]
   [schema.core :as s]))

(defprotocol ChannelProvider
  (channel [_] "Return a channel"))

(defrecord Channel [channel]
  Lifecycle
  (start [component] (assoc component :channel channel))
  (stop [component] component)
  ChannelProvider
  (channel [component] channel))

(defn new-channel [& {:as opts}]
  (->> opts
       (merge {:channel (chan)})
       (s/validate {:channel (s/protocol aimpl/Channel)})
       map->Channel))

;; A Mult accepts a mult and optionally takes a :tap-channel-provider
;; dependency which will provide channels that can be used with tap.

(defrecord Mult [mult-ch tap-channel-provider]
  Lifecycle
  (start [component]
    (s/validate {:tap-channel-provider (s/maybe (s/protocol ChannelProvider))}
                {:tap-channel-provider tap-channel-provider})
    component)
  (stop [component] component)
  ChannelProvider
  (channel [component]
    (tap
     mult-ch
     (if tap-channel-provider
       (channel tap-channel-provider)
       (chan)))))

(def new-mult-schema {:channel (s/protocol aimpl/Channel)})

(defn new-mult [& {:keys [channel] :or {channel (chan)} :as opts}]
  (s/validate new-mult-schema opts)
  (let [mult-ch (mult channel)]
    (map->Mult {:mult-ch mult-ch})))
