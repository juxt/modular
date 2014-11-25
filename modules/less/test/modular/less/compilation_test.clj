;; Copyright © 2014 JUXT LTD.
(ns modular.less.compilation-test
  (:require [modular.less.compilation :refer (new-less-compilation)]
            [com.stuartsierra.component :refer (start)]
            [clojure.test :refer :all]))

;; todo: convert to unit test
(deftest test-start-compilation
  (testing " start compilation"
    (start (new-less-compilation :engine :javascript
                  :less-config {:project-root "resources"
                                :source-paths ["less"]
                                :target-path "css"}))))
