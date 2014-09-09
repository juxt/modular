;; Copyright © 2014 JUXT LTD.

;; We call this project modular to make the invocation: lein new modular appname
(defproject modular/lein-template "0.5.2-SNAPSHOT"
  :description "Leiningen template for a full-featured component based app using modular extensions."
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 ;; EDN reader with location metadata
                 [org.clojure/tools.reader "0.8.3"]]
  :eval-in-leiningen true)
