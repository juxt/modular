;; Copyright © 2015 JUXT LTD.

(defproject juxt.modular/sendgrid "0.0.1"
  :description "A modular extension for sending emails via SendGrid"
  :url "https://github.com/juxt/modular/tree/master/modules/sendgrid"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.stuartsierra/component "0.2.2"]
                 [prismatic/schema "0.3.2"]
                 [juxt.modular/email "0.0.1"]
                 [http-kit "2.1.18"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0-alpha4"]]}})
