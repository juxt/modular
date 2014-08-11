;; Copyright © 2014 JUXT LTD.

(ns modular.web-template
  (:require
   [modular.template :refer (TemplateData template-data)]
   [com.stuartsierra.component :as component]
   [schema.core :as s]
   [clojure.tools.logging :refer :all]))

;; Where templates are used in the construction of web pages, a
;; component's template data may be determined by the incoming web
;; request. For example, authentication and authorization may
;; personalize the view. In these cases, we pass in the request.

(defprotocol DynamicTemplateData
  (dynamic-template-data [_ request]))

;; A special template model component supports both the static and
;; request-sensitive contributors. Contributors are specified in
;; dependencies.

(defrecord DependencyDynamicTemplateModel [static]
  DynamicTemplateData
  (dynamic-template-data [this request]
    (merge static
           (apply merge
                  (for [[k v] this]
                    (cond
                     (satisfies? TemplateData v)
                     {k (template-data v)}
                     (satisfies? DynamicTemplateData v)
                     {k (dynamic-template-data v request)}))))))

(defn new-dependency-dynamic-template-model [& {:as static}]
  (->DependencyDynamicTemplateModel static))
