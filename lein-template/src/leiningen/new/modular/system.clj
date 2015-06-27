(ns {{name}}.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.stuartsierra.component :refer (system-map system-using using)]
   [aero.core :as aero]
   [schema.core :as s]
   {{#module?.co-dependency}}
   [modular.component.co-dependency :refer (co-using system-co-using)]
   {{/module?.co-dependency}}
   {{#refers}}
   [{{namespace}} :refer ({{{refers}}})]
   {{/refers}}
   ))

(def config-schema {{config-schema}})

(defn config [] (s/validate config-schema (aero/read-config "config.edn")))

(defn make [c config config-path & {:as args}]
  (apply c (apply concat (merge args (get-in config config-path)))))

{{#modules}}
{{#fname}}
(defn {{fname}}{{{docstring}}}
  [system config]
  (assoc system
    {{#components}}
    {{key}}
    (->
      (make {{constructor}} config {{config-path}} {{#args}} {{{.}}}{{/args}})
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
      (-> {}{{#modules}}{{#fname}}
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
  []
  (-> (new-system-map (config))
      (system-using (new-dependency-map))
      {{#module?.co-dependency}}
      (system-co-using (new-co-dependency-map))
      {{/module?.co-dependency}}
      ))
