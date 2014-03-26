;; Copyright © 2014, JUXT LTD. All Rights Reserved.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(defproject juxt/modular "0.2.0"
  :description "FIXME: write description"
  :url "http://github.com/mastodonc/kixi.stentor"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :plugins [[lein-sub "0.2.3"]]

  :sub [
        "modules/ring" ; Ring interface
        "modules/http-kit"        ; HTTP server (with client library)
        "modules/bidi" ; URI routing library
        "modules/cassandra" ; C* support based on cassaforte
        "modules/cylon" ; Authentication

        ;;"modules/cljs-builder" ; ClojureScript compilation
        ;;        "extensions/async" ; core.async channels that can be shared by dependants
        ;;        "extensions/compojure" ; URI routing library
        ;;        "extensions/jetty" ; HTTP server
        ;;        "extensions/netty" ; Generic network server library
        ;;        "extensions/netty-mqtt" ; MQTT support for Netty
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

  :aliases {"deploy-all" ["do" "deploy" "clojars," "sub" "deploy" "clojars"]
            "install-all" ["do" "install," "sub" "install"]})
