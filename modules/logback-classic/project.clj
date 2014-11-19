;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/logback-classic "0.5.3"
  :description "A modular extension that lets you config logback using a clojure map"
  :url "https://github.com/juxt/modular/tree/master/modules/logback-classic"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/component "0.2.2"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.clojure/tools.reader "0.8.12"]
                 [prismatic/schema "0.3.2"]
                 [ch.qos.logback/logback-classic "1.0.7" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jcl-over-slf4j "1.7.2"]
                 [org.slf4j/jul-to-slf4j "1.7.2"]
                 [org.slf4j/log4j-over-slf4j "1.7.2"]])
