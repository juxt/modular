(defproject juxt/modular "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://github.com/mastodonc/kixi.stentor"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :plugins [[lein-sub "0.2.3"]]

  :sub [
        "modules/http-kit"        ; HTTP server (with client library)

        ;;        "extensions/async" ; core.async channels that can be shared by dependants
        ;;        "extensions/bidi" ; URI routing library
        ;;        "extensions/cljs-builder" ; ClojureScript compilation
        ;;        "extensions/compojure" ; URI routing library
        ;;        "extensions/jetty" ; HTTP server
        ;;        "extensions/netty" ; Generic network server library
        ;;        "extensions/netty-mqtt" ; MQTT support for Netty
        ;;        "extensions/ring" ; Ring utilities
        ;;        "extensions/stencil" ; Templating library
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

  :aliases {"deploy-all" ["do" "sub" "deploy" "clojars," "deploy" "clojars"]
            "install-all" ["do" "sub" "install," "install"]})
