(defproject {{name}} "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies
  [
   [org.clojure/clojure "1.6.0"]
   [org.clojure/tools.reader "0.8.3"]
   [org.clojure/tools.logging "0.2.6"]
   [com.stuartsierra/component "0.2.1"]
   [prismatic/schema "0.2.1"]
   [prismatic/plumbing "0.2.2"]

   [juxt.modular/maker "0.5.0"]
   [juxt.modular/wire-up "0.5.0"]

   {{#dependencies}}
   {{{.}}}
   {{/dependencies}}

   ;; transitive for bootstrap-login-form, while depending on bootstrap-login-form src
   [garden "1.1.5" :exclusions [org.clojure/clojure]]
   ;; likewise for http-kit
   [http-kit "2.1.13"]

   ;; likewise for cylon
   [cheshire "5.3.1"]
   [clj-jwt "0.0.8"]
   ]

  :main {{name}}.main

  :repl-options {:init-ns user
                 :welcome (println "Type (dev) to start")}

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["dev"
                                  "{{modular-dir}}/modules/bidi/src"
                                  "{{modular-dir}}/modules/ring/src"
                                  "{{modular-dir}}/modules/template/src"
                                  "{{modular-dir}}/modules/web-template/src"
                                  "{{modular-dir}}/modules/http-kit/src"
;;                                  "{{modular-dir}}/modules/cljs/src"
                                  "{{modular-dir}}/modules/menu/src"
                                  "{{cylon-dir}}/src"
                                  "{{cylon-dir}}/contrib/bootstrap-login-form/src"]}})
