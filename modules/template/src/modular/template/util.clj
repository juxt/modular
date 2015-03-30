;; Copyright © 2014-2015 JUXT LTD.

(ns modular.template.util
  (:require
   [clojure.walk :refer (postwalk)]))

(defprotocol TemplateDataValue
  (as-template-data-value [_]
    "Turn Clojure things into strings (useful for a Mustache template model)"))

(extend-protocol TemplateDataValue
  nil
  (as-template-data-value [_] "")
  clojure.lang.Keyword
  (as-template-data-value [k] (name k))
  Object
  (as-template-data-value [s] s ))

(defn stringify-map-values [a]
  (if (and (vector? a) (= (count a) 2))
    [(first a) (as-template-data-value (second a))]
    a))

(defn model->template-model
  "Some pre-processing on the model provided by Cylon"
  [model]
  (postwalk stringify-map-values model))
