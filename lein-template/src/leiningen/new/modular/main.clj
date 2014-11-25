(ns {{name}}.main
  "Main entry point"
  (:require clojure.pprint)
  (:gen-class))

(defn -main [& args]
  ;; We eval so that we don't AOT anything beyond this class
  (eval '(do (require '{{name}}.system)
             (require '{{name}}.main)
             (require 'tangrammer.component.co-dependency)
             (require 'clojure.java.browse)

             (println "Starting {{name}}")

             (let [system (tangrammer.component.co-dependency/start-system
                           ({{name}}.system/new-production-system))]

               (println "System started")
               (println "Ready...")

               (let [url (format "http://localhost:%d/" (-> system :http-listener-listener :port))]
                 (println (format "Browsing at %s" url))
                 (clojure.java.browse/browse-url url)
                 )))))
