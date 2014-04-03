;; Copyright © 2014 JUXT LTD.

(defproject juxt.modular/cassandra "0.3.0-SNAPSHOT"
  :description "A modular extension that provides support for Cassandra (via cassaforte)"
  :url "https://github.com/juxt/modular/tree/master/modules/cassandra"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[clojurewerkz/cassaforte "1.3.0-beta9"
                  :exclusions [[com.datastax.cassandra/cassandra-driver-core]]]
                 [com.datastax.cassandra/cassandra-driver-core "1.0.5"
                  :exclusions [[org.slf4j/slf4j-log4j12]
                               [log4j/log4j]]]
                 [prismatic/schema "0.2.1"]])
