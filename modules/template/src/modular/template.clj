;; Copyright © 2014 JUXT LTD.

(ns modular.template
  (:require
   [com.stuartsierra.component :as component]
   [modular.ring :refer (RingBinding)]
   [schema.core :as s]
   [clojure.tools.logging :refer :all]))

(defprotocol TemplateModel
  (template-model [_ context]))

(defprotocol Templater
  (render-template [_ template model]))

(defrecord Template [template]
  RingBinding
  (ring-binding [this req]
    {::template {(:key this)
                 (fn [req content]
                   (let [model (apply merge
                                      (for [[k v] this
                                            :when (satisfies? TemplateModel v)]
                                        {k (template-model v {:request req})}))]
                     (debugf "Template model, prior to merge with content, is %s" (pr-str model))
                     (debugf "After will be: %s" (merge model content))
                     (render-template
                      (:templater this)
                      template
                      (merge-with merge model content))))}}))

(defn new-template
  "The template argument can be a path, resource, file or whatever is
  supported by the dependant templater."
  [& {:as opts}]
  (component/using
   (->> opts
        (s/validate {:template s/Any})
        map->Template)
   [:templater]))

(defn new-keyed-template
  "The template argument can be a path, resource, file or whatever is
  supported by the dependant templater."
  [& {:as opts}]
  (component/using
   (->> opts
        (s/validate {:key s/Keyword :template s/Any})
        map->Template)
   [:templater]))

;; TODO: The issue with SingleTemplate is that it is static, it is given
;; the template as an argument. It is envisaged that other records,
;; other than SingleTemplate, which will switch the template based on
;; the request, can be developed. For now, if you have multiple
;; templates, create a SingleTemplate record for each one.

(defn wrap-template
  "Ring middleware to take the ::template function from the request and
  use it to process the response of the delegate handler. The initial
  response is considered to be a template model, and merged with other
  template models from other components."
  ([h & [k]]
     (fn [req]
       (if-let [merge-with-template (get-in req [::template k])]
         (let [resp (h req)]
           (merge resp {:body (merge-with-template req resp)}))
         (throw
          (if (::template req)
            (if k
              (ex-info (format "No template with key %s" k) {:key k})
              (ex-info (format "No default template") {}))
            (ex-info "wrap-template expects that a template is included in the system" {:key k})))))))

(defrecord TemplateModelMap []
  TemplateModel
  (template-model [this _] this))

(defn new-template-model-contributor [& {:as opts}]
  (map->TemplateModelMap opts))
