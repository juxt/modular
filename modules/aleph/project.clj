;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/aleph "0.1.4"
  :description "A modular extension that provides support for aleph"
  :url "https://github.com/juxt/modular/tree/master/modules/aleph"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[aleph "0.4.1-beta3" :exclusions [org.clojure/clojure]]
                 [prismatic/schema "1.0.4"]
                 [juxt.modular/ring "0.5.3" :exclusions [prismatic/schema]]]
  :profiles {:dev {:dependencies
                   [[org.clojure/clojure "1.7.0"]
                    [com.stuartsierra/component "0.3.1"]]}})
