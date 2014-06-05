;; Copyright © 2014 JUXT LTD.

(ns modular.template
  (:require
   [com.stuartsierra.component :as component]
   [schema.core :as s]
   [clojure.tools.logging :refer :all]))

(defprotocol TemplateData
  (template-data [_ context]))

(defrecord StaticTemplateData []
  TemplateData
  (template-data [this _] this))

(defn new-static-template-data [& {:as static}]
  (map->StaticTemplateData static))

(defrecord TemplateModel [static]
  TemplateData
  (template-data [this context]
    (merge static
           (apply merge
                  (for [[k v] this
                        :when (satisfies? TemplateModel v)]
                    {k (template-data v context)})))))

(defn new-template-model [& {:as static}]
  (->TemplateModel static))
