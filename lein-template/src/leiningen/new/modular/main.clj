(ns {{name}}.main
  "Main entry point"
  (:require clojure.pprint)
  (:gen-class))

;; We wrap the system in a system wrapper so that we can define a
;; print-method that will avoid recursion.
(defrecord SystemWrapper [p]
  clojure.lang.IDeref
  (deref [this] (deref p))
  clojure.lang.IFn
  (invoke [this a] (p a)))

(defmethod print-method SystemWrapper [_ writer]
  (.write writer "#system \"<system>\""))

(defmethod print-dup SystemWrapper [_ writer]
  (.write writer "#system \"<system>\""))

(. clojure.pprint/simple-dispatch addMethod SystemWrapper
   (fn [x]
     (print-method x *out*)))

(defn new-system-wrapper []
  (->SystemWrapper (promise)))

(defn -main [& args]
  ;; We eval so that we don't AOT anything beyond this class
  (eval '(do (require '{{name}}.system)
             (require '{{name}}.main)
             (require 'com.stuartsierra.component)
             (require 'clojure.java.browse)

             (println "Starting {{name}}")

             (let [systemref ({{name}}.main/new-system-wrapper)
                   system (com.stuartsierra.component/start
                           ({{name}}.system/new-production-system systemref))
                   url (format "http://localhost:%d/" (-> system :webserver :port))]

               (deliver systemref system)

               (println "System started")
               (println "Ready...")

               (println (format "Browsing at %s" url))
               (clojure.java.browse/browse-url url)))))
