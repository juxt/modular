;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/datomic "0.1.0-SNAPSHOT"
  :description "A modular extension that provides support for Datomic"
  :url "https://github.com/juxt/modular/tree/master/modules/bidi"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[juxt/modular "0.2.0-SNAPSHOT"]
                 [prismatic/schema "0.2.1"]
                 [juxt/datomic-extras "1.0.3"
                  :exclusions [org.slf4j/slf4j-nop
                               org.slf4j/jul-to-slf4j
                               org.slf4j/jcl-over-slf4j
                               org.slf4j/log4j-over-slf4j]]
                 ])
