;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/bidi "0.3.0-SNAPSHOT"
  :description "A modular extension that provides support for bidi routing"
  :url "https://github.com/juxt/modular/tree/master/modules/bidi"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[juxt/modular "0.3.0-SNAPSHOT"]
                 [bidi "1.10.2"]
                 [juxt.modular/ring "0.3.0-SNAPSHOT"]
                 [prismatic/schema "0.2.1"]])
