;; Copyright © 2014 JUXT LTD.

(ns modular.template
  (:require
   [com.stuartsierra.component :as component]
   [schema.core :as s]
   [clojure.tools.logging :refer :all]))

;; Terminology: Any component can return template /data/, which is a
;; simple map (or record). When multiple sources of template data are
;; combined together, for the purposes of template rendering, we call it
;; a template /model/.

(defprotocol TemplateData
  (template-data [_]
    "Returns a map"))

(defrecord StaticTemplateData []
  TemplateData
  (template-data [this] this))

(defn new-static-template-data
  "Construct a component that will provide the given k-v arguments as
  template data"
  [& {:as static}]
  (map->StaticTemplateData static))

(defrecord TemplateModel [static]
  TemplateData
  (template-data [this]
    (merge static
           (apply merge
                  (for [[k v] this
                        :when (satisfies? TemplateData v)]
                    {k (template-data v)})))))

(defn new-template-model [& {:as static}]
  (->TemplateModel static))
