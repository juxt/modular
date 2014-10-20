(ns {{name}}.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.tools.reader :refer (read)]
   [clojure.string :as str]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
   [com.stuartsierra.component :refer (system-map system-using using)]
   [modular.maker :refer (make)]
   {{#refers}}
   [{{namespace}} :refer ({{{refers}}})]
   {{/refers}}
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
  (config-from (io/file (System/getProperty "user.home") ".{{name}}.edn")))

(defn ^:private config-from-classpath
  []
  (if-let [res (io/resource "{{name}}.edn")]
    (config-from (io/file res))
    {}))

(defn config
  "Return a map of the static configuration used in the component
  constructors."
  []
  (merge (config-from-classpath)
         (user-config)))

{{#assemblies}}
(defn {{fname}} [system config]
  (assoc system
    {{#components}}
    {{key}}
    (using
      (make {{constructor}} config
{{#args}}
       {{k}} {{{v}}}
{{/args}})
      {{using}})

{{/components}}))

{{/assemblies}}
(defn new-system-map
  [config]
  (apply system-map
    (apply concat
      (-> {}
          {{#assemblies}}
          ({{fname}} config)
          {{/assemblies}}
          ))))

(defn new-dependency-map
  []
  {{dependency-map}})

(defn new-production-system
  "Create the production system"
  []
  (system-using (new-system-map (config))
                (new-dependency-map)))
