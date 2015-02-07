;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/bidi "0.8.0-SNAPSHOT"
  :description "A modular extension that provides support for bidi routing"
  :url "https://github.com/juxt/modular/tree/master/modules/bidi"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/component "0.2.1"]
                 [bidi "1.18.0"]
                 [juxt.modular/ring "0.5.2"]
                 [prismatic/schema "0.3.3"]
                 [prismatic/plumbing "0.3.5"]]
  :profiles {:dev
             {:dependencies [[ring-mock "0.1.5"]]}})
