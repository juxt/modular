(ns {{name}}.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.tools.reader :refer (read)]
   [clojure.string :as str]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
   [com.stuartsierra.component :refer (system-map system-using using)]
   {{#module?.co-dependency}}
   [tangrammer.component.co-dependency :refer (co-using system-co-using)]
   {{/module?.co-dependency}}
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
{{#modules}}
{{#fname}}
(defn {{fname}}{{{docstring}}}
  [system config]
  (assoc system
    {{#components}}
    {{key}}
    (->
      (make {{constructor}} config{{#args}} {{{.}}}{{/args}})
      (using {{using}})
      {{#module?.co-dependency}}
      (co-using {{co-using}})
      {{/module?.co-dependency}}
      )
{{/components}}))
{{/fname}}

{{/modules}}
(defn new-system-map
  [config]
  (apply system-map
    (apply concat
      (-> {}
          {{#modules}}{{#fname}}
          ({{fname}} config){{/fname}}{{/modules}}
          ))))

(defn new-dependency-map
  []
  {{#dependencies}}
  {{.}}

  {{/dependencies}})

(defn new-co-dependency-map
  []
  {{#co-dependencies}}
  {{.}}

  {{/co-dependencies}})

(defn new-production-system
  "Create the production system"
  ([opts]
   (-> (new-system-map (merge (config) opts))
     (system-using (new-dependency-map))
     {{#module?.co-dependency}}
     (system-co-using (new-co-dependency-map))
     {{/module?.co-dependency}}
     ))
  ([] (new-production-system {})))
