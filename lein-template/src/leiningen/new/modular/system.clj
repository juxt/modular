(ns {{name}}.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.tools.reader :refer (read)]
   [clojure.string :as str]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]

   [com.stuartsierra.component :as component :refer (system-map system-using)]

   [modular.maker :refer (make)]

   {{#requires}}
   [{{namespace}} :refer ({{{refers}}})]
   {{/requires}}

   ))

(defn ^:private read-file
  [f]
  (read
   ;; This indexing-push-back-reader gives better information if the
   ;; file is misconfigured.
   (indexing-push-back-reader
    (java.io.PushbackReader. (io/reader f)))))

(defn ^:private config-from
  [f]
  (if (.exists f)
    (read-file f)
    {}))

(defn ^:private user-config
  []
  (config-from (io/file (System/getProperty "user.home") ".presentation.edn")))

(defn ^:private config-from-classpath
  []
  (if-let [res (io/resource "presentation.edn")]
    (config-from (io/file res))
    {}))

(defn config
  "Return a map of the static configuration used in the component
  constructors."
  []
  (merge (config-from-classpath)
         (user-config)))

(defn new-base-system-map
  [config]
  (system-map
   {{#components}}
   {{component}} (make {{constructor}} config{{{args}}})
   {{/components}}
   ))

(defn new-base-dependency-map [system-map]
  {{dependency-map}})

(defn new-production-system
  "Create the production system"
  []
  (let [s-map (new-base-system-map (config))
        d-map (new-base-dependency-map s-map)]
    (with-meta
      (component/system-using s-map d-map)
      {:dependencies d-map})))
