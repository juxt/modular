;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/email "0.0.1"
  :description "A modular extension that represents the an email interface"
  :url "https://github.com/juxt/modular/tree/master/modules/email"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/component "0.2.2"]
                 [prismatic/schema "0.3.2"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0-alpha4"]]}})
