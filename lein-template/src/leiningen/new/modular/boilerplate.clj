(ns {{name}}.boilerplate
  (:require
   [com.stuartsierra.component :as component]
   [modular.bootstrap :refer (ContentBoilerplate wrap-content-in-boilerplate)]
   [modular.web-template :refer (dynamic-template-data)]
   [plumbing.core :refer (<-)]
   [clostache.parser :refer (render-resource)]
   ))

(defrecord TemplateBasedBoilerplate []
  ContentBoilerplate
  (wrap-content-in-boilerplate [this req content]
    (let [model (dynamic-template-data (:template-model this) req)]
       (render-resource
        "templates/page.html.mustache"
        (assoc model :content content)))))

(defn new-boilerplate [& {:as opts}]
  (->> opts
       map->TemplateBasedBoilerplate
       (<- (component/using [:template-model]))))
