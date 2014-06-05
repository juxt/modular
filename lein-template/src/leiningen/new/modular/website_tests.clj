(ns {{name}}.website-tests
  (:require
   [clojure.test :refer :all]
   [com.stuartsierra.component :as component]
   dev))

(def ^:dynamic *system* nil)

(defmacro with-system [system & body]
  `(let [s# ~system]
     (try
       (component/start s#)
       (binding [*system* s#] ~@body)
       (finally
         (component/stop s#)))))

(defn system-fixture [f]
  (with-system (dev/new-dev-system)
    (f)))

(use-fixtures :once system-fixture)

(deftest server
  (testing "webserver"
    (println "System is " (type *system*))
    (is (= (+ 2 2) 4))))
