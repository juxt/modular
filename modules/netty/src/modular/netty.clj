(ns modular.netty
  (:require
   [com.stuartsierra.component :as component])
  (:import
   (io.netty.channel.nio NioEventLoopGroup)
   (io.netty.bootstrap ServerBootstrap)
   (io.netty.channel.socket.nio NioServerSocketChannel)
   (io.netty.channel ChannelInitializer ChannelHandler ChannelOption)))

(defprotocol NettyHandlerProvider
  (netty-handler [_])
  (priority [_]
    "Will be order by priority, lower value is higher priority. A
     handler with a higher priority will be placed in the handler chain
     first and get to process messages before handlers of lower
     priority."))

 (defrecord NettyServer [port so-options]
   component/Lifecycle
   (start [this]

     (let [handlers (->> this vals
                         (filter (partial satisfies? NettyHandlerProvider))
                         (sort-by priority)
                         (map netty-handler)
                         ;; we doall so that errors in netty-handler occur on startup.
                         doall)]

       (when (empty? handlers)
         (throw (ex-info "No netty handler dependencies on server" {:component this})))

       (let [boss-group (NioEventLoopGroup.)
             worker-group (NioEventLoopGroup.)]
         (let [b (ServerBootstrap.)]
           (-> b
               (.group boss-group worker-group)
               (.channel NioServerSocketChannel)
               (.childHandler
                (proxy [ChannelInitializer] []
                  (initChannel [ch]
                    ;;(debugf "Initializing channel with handlers: %s" (vec handlers))
                    (-> ch (.pipeline) (.addLast (into-array ChannelHandler (map (fn [f] (if (fn? f) (f) f)) handlers)))))))
               (.option ChannelOption/SO_BACKLOG (int (or (:so-backlog so-options) 128)))
               (.childOption ChannelOption/SO_KEEPALIVE (or (:so-keepalive so-options) true)))

           (assoc this
             :channel (.bind b port)
             :boss-group (NioEventLoopGroup.)
             :worker-group (NioEventLoopGroup.))))))

   (stop [this]
     (let [fut (:channel this)]
       (.awaitUninterruptibly fut)      ; await for it to be bound
       (-> fut (.channel) (.close) (.sync)))
     (.shutdownGracefully (:worker-group this))
     (.shutdownGracefully (:boss-group this))
     this))

(defn new-netty-server
  [{:keys [port] :as opts}]
  (assert port)
  (map->NettyServer opts))
