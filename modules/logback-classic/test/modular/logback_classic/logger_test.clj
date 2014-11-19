;; Copyright © 2014 JUXT LTD.
(ns modular.logback-classic.logger-test
  (:require [clojure.test :refer :all]
            [modular.logback-classic.logger :refer (new-logger-conf)]
            [clojure.tools.logging :refer :all]
            [com.stuartsierra.component :as component :refer (start stop)]))


(deftest test-logger-component
  "A simple not unit test that writes messages to file and console"
  (testing "loger-conf component"
    (let [c (new-logger-conf "logs/test.log" :loggers {"modular" :info
                                                       "modular.logback-classic" :warn})]
      (start c)
      (warn "testing modular.logback-classic output")
      (stop c))))
