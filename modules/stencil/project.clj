;; Copyright © 2015 JUXT LTD.

(defproject juxt.modular/stencil "0.1.0"
  :description "A modular extension that provides a Stencil templater for juxt.modular/template"
  :url "https://github.com/juxt/modular/tree/master/modules/stencil"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/component "0.2.2"]
                 [stencil "0.4.0" :exclusions [slingshot]]
                 [juxt.modular/template "0.6.3"]
                 ])
