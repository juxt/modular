;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/aleph "0.1.1"
  :description "A modular extension that provides support for aleph"
  :url "https://github.com/juxt/modular/tree/master/modules/aleph"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[aleph "0.4.0" :exclusions [org.clojure/clojure]]
                 [prismatic/schema "0.4.3"]
                 [juxt.modular/ring "0.5.2" :exclusions [prismatic/schema ]]]
  :profiles {:dev {:dependencies
                   [[org.clojure/clojure "1.7.0-RC1"]
                    [com.stuartsierra/component "0.2.3"]]}})
