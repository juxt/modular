;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/cljs "0.4.0-SNAPSHOT"
  :description "A modular extension that provides support for ClojureScript building and Javascript serving"
  :url "https://github.com/juxt/modular/tree/master/modules/cljs"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[juxt.modular/bidi "0.4.0-SNAPSHOT"]
                 [juxt.modular/template "0.1.0-SNAPSHOT"]
                 [prismatic/schema "0.2.1"]
                 [thheller/shadow-build "0.5.0" :exclusions [org.clojure/clojurescript]]])
