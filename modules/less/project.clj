;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/less "0.5.3"
  :description "A modular extension that lets you compile less files into css."
  :url "https://github.com/juxt/modular/tree/master/modules/less"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [prismatic/schema "0.3.3"]
                 [com.stuartsierra/component "0.2.2"]
                 [malcolmsparks/lein-less "1.7.3"]])
