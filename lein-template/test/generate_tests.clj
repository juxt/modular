(ns generate-tests
  (:require
   [clojure.test :refer :all]
   [clojure.java.io :as io]
   [leiningen.new.modular :refer (modular)]))

(defn delete-dir [dir inclusive?]
  (doseq [f (.listFiles dir)]
    (cond
     (.isDirectory f) (delete-dir f true)
     (.isFile f)
     (io/delete-file f)))
  (when inclusive? (io/delete-file dir true)))

(defn get-tmp-dir []
  (io/file (System/getProperty "java.io.tmpdir") "modular-lein-template"))

(defn generate-project [name & args]
  (let [dir (get-tmp-dir)
        projectdir (io/file dir name)]
    (delete-dir projectdir false)
    (binding [leiningen.new.templates/*dir* projectdir]
      (apply modular name args))))

#_(defn project-fixture [f]
  (generate-project "myapp")
  (f))

#_(use-fixtures :once project-fixture)

(deftest website-tests
  (generate-project "website")
  (testing "project file exists"
    (is (.exists (io/file (get-tmp-dir) "website/project.clj")))))

(deftest website-with-login-tests
  (generate-project "website-with-login" "+cylon/login")
  (testing "project file exists"
    (is (.exists (io/file (get-tmp-dir) "website-with-login/project.clj")))))
