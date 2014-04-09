;; Copyright © 2014 JUXT LTD.

(ns ^{:clojure.tools.namespace.repl/unload false
      :clojure.tools.namespace.repl/load false}
  modular.index)

;; A component that satisfying this Index protocol declares that it
;; provides an index for components satisfying one or more
;; protocols. For example, a Menu component may be an index of all
;; MenuItem components. A contents or index page could be generated from
;; all the Page components present in the system.

;; An application can use these declarations to infer a dependency tree
;; between the components satisfying Index and components satisfying one
;; or more of the protocols declared by it.

;; One of the characteristics of modular applications is dynamic
;; component discovery that allows applications to be extended via the
;; introduction of new components. Therefore we must ensure that the
;; coupling between existing components is minimised.

(defprotocol Index
  (satisfying-protocols [this]))

(defn add-index-dependencies
  [dependency-map system-map]
  (merge-with merge
              dependency-map
              (reduce
               (fn [acc [p q]]
                 (update-in acc [p] assoc q q))
               {}
               (for [[k v] system-map :when (satisfies? Index v)
                     prot (satisfying-protocols v)
                     [q impl] system-map :when (satisfies? prot impl)]
                 [k q]))))
