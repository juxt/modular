;; Copyright © 2014 JUXT LTD.

(ns ^{:clojure.tools.namespace.repl/unload false
      :clojure.tools.namespace.repl/load false}
  modular.core
  (:require
   [modular.index :refer (Index satisfying-protocols)]
   [clojure.pprint :refer (pprint)]
   [com.stuartsierra.component :as component]))

(defn make-args
  "In modular, constructors use the variadic keyword arguments
  call-convention. This function allows us to formulate these arguments
  from a config map and a list of specified keys. Each key can take a
  default value, or nil if no value should be passed. The value will
  then be determined by the constructor itself, not the calling code."
  [cfg & {:as args}]
  (as-> args %
        (merge % cfg)
        (select-keys % (keys args))
        (seq %)
        (remove (comp nil? second) %)
        (apply concat %)))

(defn make
  "Call the constructor with default keyword arguments, each of which is
   overridden if the entry exists in the given config map."
  ([ctr config & kvs]
     (assert fn? ctr)
     (assert (not (keyword? config)) "Please specify a config map as the second argument to make")
     (apply ctr (apply make-args config kvs)))
  ;; If only the constructor is specified, do the sensible thing.
  ([ctr]
     (make ctr {})))

(defn normalize-dependency-map
  "component/using and system/using accept vectors as well as maps. This
  makes it difficult to process (merge, extract, etc.) dependency
  trees. Use this function to normalise so that only the map form is
  used."
  [m]
  (reduce-kv
   (fn [s k v]
     (assoc s k
            (if (vector? v)
              (apply zipmap (repeat 2 v))
              v)))
   {} m))

(defn autowire-dependencies-satisfying
  "Return a system definition, adding dependencies between the given
  key (of the dependant) and any components in the given system map that
  satisfy the given protocol."
  [dependency-map system-map dependant-key proto]
  (normalize-dependency-map {dependant-key (vec (keep (fn [[k v]] (when (satisfies? proto v) k)) (seq system-map)))}))

(defn interpose-component
  "Splice in a component between a dependant component and its
  dependencies. The dependant component will depend on the interposed
  component and the interposed component will depend on the dependant's
  former dependencies. For example, (A -> B, A Z) becomes A -> Z -> B"
  [dependency-map dependant-key component-key]
  (-> dependency-map
      (update-in [component-key] merge (into {} (remove #(= (second %) component-key) (seq (or (dependant-key dependency-map) {})))))
      (assoc dependant-key {component-key component-key})))

(defn merge-dependencies [dependency-map & new-deps]
  (apply merge-with merge dependency-map (map normalize-dependency-map new-deps)))
