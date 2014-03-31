;; Copyright © 2014, JUXT LTD. All Rights Reserved.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns ^{:clojure.tools.namespace.repl/unload false
      :clojure.tools.namespace.repl/load false}
  modular.core
  (:require
   [modular.protocols :refer (types)]
   [clojure.pprint :refer (pprint)]
   [com.stuartsierra.component :as component]))

(defn normalize
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

(defn add-index-dependencies
  [dependency-map system]
  (reduce
   (fn [acc [p q]] (update-in acc [p] assoc q q))
   dependency-map
   (for [[k v] system :when (satisfies? modular.protocols/Index v)
         prot (types v)
         [q impl] system :when (satisfies? prot impl)]
     [k q])))

(defn autowire-dependencies-satisfying
  "Return a dependency map between the given key (of the dependant) and
  any components in the given system map that satisfy the given
  protocol."
  [system-map dependant-key proto]
  (normalize {dependant-key (vec (keep (fn [[k v]] (when (satisfies? proto v) k)) (seq system-map)))}))

(defn system-using
  "A pure convenience function for projects using add-index-dependencies
  only. Most projects will call component/system-using directly."
  [system-map dependency-map]
  (component/system-using system-map (-> dependency-map normalize (add-index-dependencies system-map))))
