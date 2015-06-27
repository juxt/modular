(ns dev
  (:require
   [clojure.pprint :refer (pprint)]
   [clojure.reflect :refer (reflect)]
   [clojure.repl :refer (apropos dir doc find-doc pst source)]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [clojure.java.io :as io]
   [com.stuartsierra.component :as component]
   [schema.core :as s]
   {{#module?.co-dependency}}
   [modular.component.co-dependency :as co-dependency]
   {{/module?.co-dependency}}
   [{{name}}.system :refer (config new-system-map new-dependency-map new-co-dependency-map)]
   {{#dev-refers}}
   [{{namespace}} :refer ({{{refers}}})]
   {{/dev-refers}}))

(def system nil)

(defn new-dev-system
  "Create a development system"
  []
  (let [config (config)
        s-map (->
               (new-system-map config))]
    (-> s-map
        (component/system-using (new-dependency-map))
        {{#module?.co-dependency}}
        (co-dependency/system-co-using (new-co-dependency-map))
        {{/module?.co-dependency}}
        )))

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system
    (constantly (new-dev-system))))

(defn check
  "Check for component validation errors"
  []
  (let [errors
        (->> system
             (reduce-kv
              (fn [acc k v]
                (assoc acc k (s/check (type v) v)))
              {})
             (filter (comp some? second)))]

    (when (seq errors) (into {} errors))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root
   #'system
   {{#module?.co-dependency}}
   co-dependency/start-system
   {{/module?.co-dependency}}
   {{^module?.co-dependency}}
   component/start
   {{/module?.co-dependency}})
  (when-let [errors (check)] (println "System integrity errors:" errors)))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s)))))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start)
  :ok
  )

(defn reset []
  (stop)
  (refresh :after 'dev/go))

;; REPL Convenience helpers

{{{dev-snippets}}}
