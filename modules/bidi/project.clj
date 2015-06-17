;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/bidi "0.9.3"
  :description "A modular extension that provides support for bidi routing"
  :url "https://github.com/juxt/modular/tree/master/modules/bidi"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/component "0.2.3"]
                 [bidi "1.19.1"]
                 [juxt.modular/ring "0.5.3"]
                 [prismatic/schema "0.4.2"]]
  :profiles {:dev
             {:dependencies [[ring-mock "0.1.5"]]}})
