(ns dev-components
  (:require
   [com.stuartsierra.component :refer (Lifecycle using)]
   [schema.core :as s]
   [plumbing.core :refer (<-)]))

(defn wrap-schema-validation [h]
  (fn [req]
    (s/with-fn-validation
      (h req))))
