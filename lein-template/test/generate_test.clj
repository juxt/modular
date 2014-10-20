(ns generate-test
  (:require
   [clojure.test :refer :all]
   [clojure.java.io :as io]
   [leiningen.new.modular :refer (modular)]))

(defn delete-dir
  ;; Sometimes we don't want to delete the top-level directory because
  ;; it forces us to cd back into it from the shell when testing.
  ([dir] (delete-dir dir false))
  ([dir inclusive?]
     (when (.exists dir)
       (doseq [f (.listFiles dir)]
         (cond
          (.isDirectory f) (delete-dir f true)
          (.isFile f)
          (io/delete-file f)))
       (when inclusive? (io/delete-file dir true)))))

(defn get-tmp-dir []
  (doto
      (io/file (System/getProperty "java.io.tmpdir") "modular-lein-template")
    (.mkdirs)))

(defn generate-project [name & args]
  (println "Generate project")
  (let [dir (get-tmp-dir)
        projectdir (io/file dir name)]
    (println "Deleting directory" projectdir)
    (delete-dir projectdir)
    (binding [leiningen.new.templates/*dir* projectdir
              leiningen.new.templates/*force?* true]
      (println "Applying modular")
      (try
        (apply modular name args)
        (catch Exception e (.printStackTrace e))))))

#_(defn project-fixture [f]
  (generate-project "myapp")
  (f))

#_(use-fixtures :once project-fixture)

(deftest website-tests
  (generate-project "website")
  (testing "project file exists"
    (is (.exists (io/file (get-tmp-dir) "website/project.clj")))))

#_(deftest website-with-login-tests
  (generate-project "website-with-login" "+cylon/login")
  (testing "project file exists"
    (is (.exists (io/file (get-tmp-dir) "website-with-login/project.clj")))))
