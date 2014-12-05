;; Copyright © 2014 JUXT LTD.

(ns modular.less.compilation-test
  (:require [modular.less :refer (new-less-compiler)]
            [clojure.test :refer :all]
            [clj-less.less :refer (run-compiler)]
            [clojure.java.io :as io]))

#_(deftest test-bootstrap-resource
  (let [outfile "/tmp/bootstrap.css"]
    (when (.exists (io/file outfile))
      (io/delete-file outfile))
    (time
     (run-compiler
      {:engine :nashorn
       :source-path "jar:file:/home/malcolm/.m2/repository/org/webjars/bootstrap/3.3.0/bootstrap-3.3.0.jar!/META-INF/resources/webjars/bootstrap/3.3.0/less/bootstrap.less"
       :target-path outfile}))
    (let [outfile (io/file outfile)]
      (is (.exists outfile))
      (is (.isFile outfile))
      (is (> (.length outfile) 2000)))))

;;(slurp (io/resource "META-INF/resources/webjars/bootstrap/3.3.0/less/bootstrap.less"))

#_(deftest test-custom-bootstrap-resource
  (let [outfile "/tmp/custom-bootstrap.css"]
    (when (.exists (io/file outfile))
      (io/delete-file outfile))
    (run-compiler
     {:engine :nashorn
      :source-path "custom-bootstrap.less"
      :target-path outfile
      :loader
      (fn [x]
        (println "Loading" x)
        (slurp
         (if-let [bootstrap-path (second (re-matches #"../bootstrap/(.*)" x))]
           (io/resource (str "META-INF/resources/webjars/bootstrap/3.3.0/" bootstrap-path))
           (str "resources/" x))))})
    (let [outfile (io/file outfile)]
      (is (.exists outfile))
      (is (.isFile outfile))
      (is (> (.length outfile) 2000)))))

#_(deftest test-sample-resource
  (let [outfile "/tmp/custom-bootstrap.css"]
    (when (.exists (io/file outfile))
      (io/delete-file outfile))
    (run-compiler
     {:engine :nashorn
      :project-root "resources"
      :source-path "resources/sample.less"
      :target-path outfile})
    (let [outfile (io/file outfile)]
      (is (.exists outfile))
      (is (.isFile outfile))
      (is (> (.length outfile) 50)))))
