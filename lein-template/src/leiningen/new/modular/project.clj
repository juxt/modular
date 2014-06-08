(defproject {{name}} "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies
  [
   [org.clojure/clojure "1.6.0"]
   [org.clojure/tools.reader "0.8.3"]
   [org.clojure/tools.logging "0.2.6"]
   [com.stuartsierra/component "0.2.1"]

   [juxt.modular/maker "0.1.0"]
   [juxt.modular/wire-up "0.1.0"]

   {{#dependencies}}
   {{{.}}}
   {{/dependencies}}

   ]

  :main {{name}}.main

  :repl-options {:init-ns user
                 :welcome (println "Type (dev) to start")}

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["dev"
                                  "{{modular-dir}}/modules/bidi/src"
                                  "{{modular-dir}}/modules/template/src"
                                  "{{modular-dir}}/modules/cljs/src"
                                  "{{cylon-dir}}/src"]}})
