;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/cljs "0.6.0"
  :description "A modular extension that provides support for ClojureScript building and Javascript serving"
  :url "https://github.com/juxt/modular/tree/master/modules/cljs"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[juxt.modular/bidi "0.9.0"]
                 [juxt.modular/template "0.6.2"]
                 [prismatic/schema "0.3.5"]
                 [thheller/shadow-build "0.5.0" :exclusions [org.clojure/clojurescript]]])
