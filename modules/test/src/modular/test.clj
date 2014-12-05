(ns modular.test
  (:require
   [com.stuartsierra.component :as component]
   [clojure.tools.logging :refer :all])
  )

(def ^:dynamic *system* nil)

(defmacro with-system
  [system & body]
  `(let [s# (component/start ~system)]
     (try
       (binding [*system* s#] ~@body)
       (finally
         (component/stop s#)))))

(defn with-system-fixture
  [system]
  (fn [f]
    (with-system (system)
      (f))))
