(ns generate-test
  (:require
   [clojure.test :refer :all]
   [clojure.java.io :as io]
   [clojure.java.shell :refer (sh)]
   [leiningen.new.modular :refer (modular)])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn delete-dir
  ;; Sometimes we don't want to delete the top-level directory because
  ;; it forces us to cd back into it from the shell when testing.
  ([dir] (delete-dir dir false))
  ([dir inclusive?]
     (when (.exists dir)
       (doseq [f (.listFiles dir)]
         (cond
          (Files/isSymbolicLink (.toPath f)) (Files/delete (.toPath f))
          (.isDirectory f) (delete-dir f true)
          (.isFile f)
          (io/delete-file f)))
       (when inclusive? (io/delete-file dir true)))))

(defn get-tmp-dir []
  (doto
      (io/file (System/getProperty "java.io.tmpdir") "modular-lein-template")
    (.mkdirs)))

(defn generate-project [name app-template & args]
  (println "Generate project")
  (let [dir (get-tmp-dir)
        projectdir (io/file dir name)]
    (println "Deleting directory" projectdir)
    (delete-dir projectdir)
    (binding [leiningen.new.templates/*dir* projectdir
              leiningen.new.templates/*force?* true]
      (println "Applying modular")
      (try
        (apply modular name app-template args)
        (catch Exception e (.printStackTrace e))))))

(defn generate-checkout [name src project]
  (println "Generate checkout:" project)
  (let [dir (get-tmp-dir)
        projectdir (io/file dir name)
        checkouts (io/file projectdir "checkouts")]
    (.mkdirs checkouts)
    (Files/createSymbolicLink
     (.resolve (.toPath checkouts) project)
     (.toPath (io/file src))
     (make-array FileAttribute 0))))

#_(defn project-fixture [f]
  (generate-project "myapp" "hello-world-web")
  (f))

#_(use-fixtures :once project-fixture)

(deftest hello-world-web-tests
  (let [name "hello-world-web"]
    (generate-project name "hello-world-web")

    (testing "project file should exist"
      (is (.exists (io/file (get-tmp-dir) (str name "/project.clj")))))))

(deftest bidi-hello-world-tests
  (let [name "bidi-hello-world-example"]
    (generate-project name "bidi-hello-world")

    (testing "project file should exist"
      (is (.exists (io/file (get-tmp-dir) (str name "/project.clj")))))))

#_(deftest templated-bidi-website-tests
  (let [name "templated-bidi-website"]
    (generate-project name "templated-bidi-website")
    (generate-checkout name "/home/malcolm/Dropbox/src/modular/modules/template" "template")
    (generate-checkout name "/home/malcolm/Dropbox/src/modular/modules/clostache" "clostache")
    (generate-checkout name "/home/malcolm/Dropbox/src/modular/modules/bidi" "modular.bidi")
    ;;(generate-checkout name "/home/malcolm/Dropbox/src/bidi" "bidi")

    (testing "project file should exist"
      (is (.exists (io/file (get-tmp-dir) (str name "/project.clj")))))))

(deftest bootstrap-cover-tests
  (let [name "bootstrap-cover-example"]
    (generate-project name "bootstrap-cover")

    (testing "project file should exist"
      (is (.exists (io/file (get-tmp-dir) (str name "/project.clj")))))))

(deftest bootstrap-dashboard-tests
  (let [name "bootstrap-dashboard-example"]
    (generate-project name "bootstrap-dashboard")

    (testing "project file should exist"
      (is (.exists (io/file (get-tmp-dir) (str name "/project.clj")))))))

(deftest sse-tests
  (let [name "sse-example"]
    (generate-project name "sse")

    (testing "project file should exist"
      (is (.exists (io/file (get-tmp-dir) (str name "/project.clj")))))))

#_(deftest website-with-login-tests
  (generate-project "website-with-login" "hello-world-web" "+cylon/login")
  (testing "project file exists"
    (is (.exists (io/file (get-tmp-dir) "website-with-login/project.clj")))))
