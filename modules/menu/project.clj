;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/menu "0.5.3"
  :description "A modular extension that provides support for menus, navbars, and other navigation elements that house hyperlinks."
  :url "https://github.com/juxt/modular/tree/master/modules/menu"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/component "0.2.1"]
                 [prismatic/schema "0.2.1"]
                 ;; menu extends Cylon's login-form
                 [cylon "0.4.1"]])
