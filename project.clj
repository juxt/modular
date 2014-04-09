;; Copyright © 2014 JUXT LTD.

(defproject juxt/modular "0.3.1"
  :description "FIXME: write description"
  :url "http://github.com/mastodonc/kixi.stentor"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :plugins [[lein-sub "0.2.3"]]

  :sub [
        "modules/ring"
        "modules/http-kit"
        "modules/bidi"
        "modules/cassandra"

        ;;"modules/cljs-builder" ; ClojureScript compilation
        ;;        "modules/async" ; core.async channels that can be shared by dependants
        ;;        "modules/compojure" ; URI routing library
        ;;        "modules/jetty" ; HTTP server
        ;;        "modules/netty" ; Generic network server library
        ;;        "modules/netty-mqtt" ; MQTT support for Netty
        ]

  :dependencies
  [[org.clojure/clojure "1.5.1"]
   [com.stuartsierra/component "0.2.1"]
   [org.clojure/tools.logging "0.2.6"]
   [ch.qos.logback/logback-classic "1.0.7" :exclusions [org.slf4j/slf4j-api]]
   [org.slf4j/jul-to-slf4j "1.7.2"]
   [org.slf4j/jcl-over-slf4j "1.7.2"]
   [org.slf4j/log4j-over-slf4j "1.7.2"]
   ]

  :repl-options {:prompt (fn [ns] (str "modular " ns "> "))}

  :aliases {"deploy-all" ["do" "deploy" "clojars," "sub" "deploy" "clojars"]
            "install-all" ["do" "install," "sub" "install"]})
