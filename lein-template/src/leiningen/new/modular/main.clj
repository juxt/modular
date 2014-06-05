(ns {{name}}.main
  "Main entry point"
  (:gen-class))

(defn -main [& args]
  ;; We eval so that we don't AOT anything beyond this class
  (eval '(do (require '{{name}}.system)
             (require 'com.stuartsierra.component)
             (require 'clojure.java.browse)

             (println "Starting {{name}}")

             (let [system (com.stuartsierra.component/start
                           ({{name}}.system/new-production-system))
                   url (format "http://localhost:%d/" (-> system :webserver :port))]
               (println "System started")
               (println "Ready...")

               (println (format "Browsing at %s" url))
               (clojure.java.browse/browse-url url)))))
