# modular test

Create a function (new-system) which creates a system, with component/system-user, and use like this :-

```clojure
(ns my.project.ns
  (:require
   [clojure.test :refer :all]
   [modular.test :refer (with-system-fixture *system*])
  ))

(defn new-system
  "Define a minimal system which is just enough for the tests in this
  namespace to run"
  []
  (component/system-using
   (component/system-map
    :some-component (new-some-component))
    {}))

(use-fixtures :each (with-system-fixture new-system))

(deftest some-test
  (println "System is" *system*))
```
