(ns modular.less.component-test
  (:require
   [clojure.test :refer :all]
   [com.stuartsierra.component :as component]
   [modular.less :refer (new-bootstrap-less-compiler)]
   [modular.test :refer (with-system-fixture *system*)]
   [clojure.java.io :as io]))

(defn new-system
  "Define a minimal system which is just enough for the tests in this
  namespace to run"
  []
  (component/system-using
   (component/system-map
    :less (new-bootstrap-less-compiler
           :resource-dir "test-resources"
           :target-path "target/test/less/bootstrap.less"))
   {}))

(use-fixtures :each (with-system-fixture new-system))

(deftest target-file-should-exist
  (is (.exists (io/file "target/test/less/bootstrap.less")))
  (is (> (.length (io/file "target/test/less/bootstrap.less")) 200)))
