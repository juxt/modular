(ns generate-tests
  (:require
   [clojure.test :refer :all]
   [clojure.java.io :as io]
   [leiningen.new.modular :refer (modular)]))

(defn delete-dir [dir]
  (doseq [f (.listFiles dir)]
    (cond
     (.isDirectory f) (delete-dir f)
     (.isFile f)
     (io/delete-file f)))
  (io/delete-file dir true))

(defn get-tmp-dir []
  (io/file (System/getProperty "java.io.tmpdir") "modular-lein-template"))

(defn generate-project [name]
  (let [dir (get-tmp-dir)
        projectdir (io/file dir name)]
    (delete-dir projectdir)
    (binding [leiningen.new.templates/*dir* projectdir]
      (modular name))))

(defn project-fixture [f]
  (generate-project "foo")
  (f))

(use-fixtures :once project-fixture)

(deftest project-generation-tests
  (testing "project file exists"
    (is (.exists (io/file (get-tmp-dir) "foo/project.clj")))))
