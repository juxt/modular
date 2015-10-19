;; Copyright © 2014 JUXT LTD.

;; We call this project modular to make the invocation: lein new modular appname
(defproject modular/lein-template "0.7.2"
  :description "Leiningen template for a full-featured component based app using modular extensions."
  :url "http://modularity.org/"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies []

  :eval-in-leiningen true

  :profiles {:dev
             {:dependencies
              [[org.clojure/clojure "1.7.0"]
               ;; EDN reader with location metadata
               [org.clojure/tools.reader "0.8.3"]
               [org.clojure/tools.logging "0.3.1"]]}})
