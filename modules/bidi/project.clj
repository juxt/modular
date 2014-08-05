;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/bidi "0.5.2"
  :description "A modular extension that provides support for bidi routing"
  :url "https://github.com/juxt/modular/tree/master/modules/bidi"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/component "0.2.1"]
                 [bidi "1.10.3"]
                 [juxt.modular/ring "0.5.1"]
                 [prismatic/schema "0.2.1"]
                 [prismatic/plumbing "0.2.2"]]
  :profiles {:dev
             {:dependencies [[ring-mock "0.1.5"]]}})
