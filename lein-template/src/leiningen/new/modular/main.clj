(ns {{name}}.main
  "Main entry point"
  (:require clojure.pprint)
  (:gen-class))

(defn -main [& args]
  ;; We eval so that we don't AOT anything beyond this class
  (eval '(do (require '{{name}}.system)
             (require '{{name}}.main)
             (require 'com.stuartsierra.component)
             {{#module?.co-dependency}}
             (require 'tangrammer.component.co-dependency)
             {{/module?.co-dependency}}

             (require 'clojure.java.browse)

             (println "Starting {{name}}")

             (let [system (->
                           ({{name}}.system/new-production-system)
                           {{^module?.co-dependency}}
                           com.stuartsierra.component/start
                           {{/module?.co-dependency}}
                           {{#module?.co-dependency}}
                           tangrammer.component.co-dependency/start-system
                           {{/module?.co-dependency}}
                           )]

               (println "System started")
               (println "Ready...")

               (let [url (format "http://localhost:%d/" (-> system :http-listener-listener :port))]
                 (println (format "Browsing at %s" url))
                 (clojure.java.browse/browse-url url)
                 )))))
