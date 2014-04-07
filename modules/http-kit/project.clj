;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/http-kit "0.3.0"
  :description "A modular extension that provides support for http-kit channels"
  :url "https://github.com/juxt/modular/tree/master/modules/http-kit"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[http-kit "2.1.13"]
                 [prismatic/schema "0.2.1"]
                 [juxt/modular "0.2.0"]
                 [juxt.modular/ring "0.2.0"]
                 ])
