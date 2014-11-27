;; Copyright © 2014 JUXT LTD.

(ns modular.async
  "Make a core.async channel available as a component, so that
  channels can be named and wired up in the system configuration."
  (:require
   [com.stuartsierra.component :as component :refer (Lifecycle using)]
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
       (s/validate {(s/optional-key :channel) (s/protocol aimpl/Channel)
                    (s/optional-key :size) s/Int})
       (merge {:channel (if-let [size (:size opts)]
                          (chan size)
                          (chan))})
       map->Channel))

;; A Mult accepts a mult and optionally takes a :tap-channel-provider
;; dependency which will provide channels that can be used with tap.

(defrecord Mult [size channel-provider tap-channel-provider]
  Lifecycle
  (start [component]
    (s/validate
     {:channel-provider (s/protocol ChannelProvider)
      :tap-channel-provider (s/maybe (s/protocol ChannelProvider))}
     {:channel-provider channel-provider
      :tap-channel-provider tap-channel-provider})
    (assoc component :mult (mult (channel channel-provider))))
  (stop [component] component)
  ChannelProvider
  (channel [component]
    (tap
     (:mult component)
     (if tap-channel-provider
       (channel tap-channel-provider)
       (if size
         (chan size)
         (chan))))))

(defn new-mult [& {:as opts}]
  (->
   (->> opts
        (s/validate {(s/optional-key :size) s/Int})
        map->Mult)
   (using [:channel-provider])))
