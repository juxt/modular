(defproject {{sanitized}} "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "{{clojure-version}}"]
                 [com.stuartsierra/component "{{component-lib-version}}"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "{{tools-namespace-version}}"]]
                   :source-paths ["dev"]}})
