;; Copyright © 2014 JUXT LTD.

(ns modular.template
  (:require
   [com.stuartsierra.component :as component]
   [schema.core :as s]
   [clojure.tools.logging :refer :all]))

(defprotocol TemplateModel
  (template-model [_ context]
    "Returns a map of data, which can determined by the given context"))

(defrecord StaticTemplateModel []
  TemplateModel
  (template-model [component context] component))

(defn new-static-template-model
  "Construct a component that will provide the given k-v arguments as
  template data"
  [& {:as static}]
  (map->StaticTemplateModel static))

(defrecord AggregateTemplateModel [static]
  TemplateModel
  (template-model [component context]
    (reduce merge static
            (for [[_ v] component
                  :when (satisfies? TemplateModel v)]
              (template-model v context)))))

(defn new-aggregate-template-model [& {:as static}]
  (->AggregateTemplateModel static))

(defprotocol Templater
  (render-template [_ template model]))
