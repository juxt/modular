;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/bootstrap "0.1.0-SNAPSHOT"
  :description "A modular extension that HTML rendering of components with the Twitter Bootstrap library."
  :url "https://github.com/juxt/modular/tree/master/modules/bootstrap"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :exclusions [org.clojure/clojure]
  :dependencies [[com.stuartsierra/component "0.2.1"]
                 [prismatic/schema "0.2.1"]
                 [prismatic/plumbing "0.2.2"]
                 [hiccup "1.0.5"]
                 [garden "1.1.5"]])
