;; Copyright © 2015 JUXT LTD.

(defproject juxt.modular/postgres "0.0.1-SNAPSHOT"
  :description "A modular extension that provides support for Postgres"
  :url "https://github.com/juxt/modular/tree/master/modules/postgres"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.mchange/c3p0 "0.9.5-pre8"]
                 [postgresql/postgresql "9.3-1101.jdbc4"]])
