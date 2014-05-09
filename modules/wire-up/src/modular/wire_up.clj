;; Copyright © 2014 JUXT LTD.

(ns modular.wire-up)

(defn ensure-map
  "Turn vector style into map style, if necessary. For example: [:a :b :c] -> {:a :a, :b :b, :c :c}"
  [x]
  (if (sequential? x)
    (apply zipmap (repeat 2 x))
    x))

(defn normalize-dependency-map
  "component/using and system/using accept vectors as well as maps. This
  makes it difficult to process (merge, extract, etc.) dependency
  trees. Use this function to normalise so that only the map form is
  used."
  [m]
  (reduce-kv
   (fn [s k v]
     (assoc s k (ensure-map v)))
   {} m))

(defn autowire-dependencies-satisfying
  "Return a dependency map, adding dependencies between the given
  key (of the dependant) and any components in the given system map that
  satisfy the given protocol."
  [dependency-map system-map dependant-key proto]
  (->> {dependant-key (vec (keep (fn [[k v]] (when (and (satisfies? proto v) (not= dependant-key k)) k)) (seq system-map)))}
       normalize-dependency-map
       (merge-with merge dependency-map)))

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
