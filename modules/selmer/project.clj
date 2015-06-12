;; Copyright © 2015 JUXT LTD.

(defproject juxt.modular/selmer "0.1.0"
  :description "A modular extension that provides a Selmer templater for juxt.modular/template"
  :url "https://github.com/juxt/modular/tree/master/modules/selmer"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/component "0.2.3"]
                 [selmer "0.8.2"]
                 [juxt.modular/template "0.6.3"]])
