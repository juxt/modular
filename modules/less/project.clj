;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/less "0.1.2"
  :description "A modular extension that lets you compile Less files into CSS"
  :url "https://github.com/juxt/modular/tree/master/modules/less"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[prismatic/schema "0.3.3"]
                 [com.stuartsierra/component "0.2.2"]
                 [malcolmsparks/clj-less "1.7.3"]
                 [juxt.modular/bidi "0.7.1"]]

  :profiles {:dev {:dependencies
                   [[org.clojure/clojure "1.6.0"]
                    [org.webjars/bootstrap "3.3.0"]
                    [juxt.modular/test "0.1.0"]]}})
