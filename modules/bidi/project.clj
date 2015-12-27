;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/bidi "0.9.5"
  :description "A modular extension that provides support for bidi routing"
  :url "https://github.com/juxt/modular/tree/master/modules/bidi"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/component "0.3.1"]
                 [bidi "1.24.0"]
                 [juxt.modular/ring "0.5.3"]
                 [prismatic/schema "1.0.4"]]
  :profiles {:dev
             {:dependencies [[ring-mock "0.1.5"]]}})
