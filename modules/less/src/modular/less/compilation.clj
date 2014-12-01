;; Copyright © 2014 JUXT LTD.
(ns modular.less.compilation
  (:require [com.stuartsierra.component :refer (Lifecycle)]
            [schema.core :as s]
            [lein-less.less :refer (run-compiler)]))

(defrecord LessCompilation [engine less-config]
  Lifecycle
  (start [this]
    (run-compiler engine less-config))
  (stop [this]
    this))

(defn new-less-compilation [& {:as opts}]
  (->> opts
       (merge {:engine :javascript})
       (s/validate {:engine (s/enum :javascript :rhino :nashorn)
                    :less-config {:project-root s/Str
                                  :source-path s/Str
                                  :target-path s/Str}})
       map->LessCompilation))
