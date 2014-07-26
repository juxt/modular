(ns {{name}}.example-page
  (:require
   [clojure.string :as str]
   [com.stuartsierra.component :refer (using)]
   [modular.menu :refer (MenuItems)]
   [modular.bidi :refer (WebService)]
   [modular.web-template :refer (dynamic-template-data)]
   [clostache.parser :refer (render-resource)]
   [plumbing.core :refer (<-)]
   [hiccup.core :refer (html)]))

(defrecord ExamplePage [label content key path order]
  MenuItems
  (menu-items [_] [{:label label :target key :order order}])
  WebService
  (request-handlers [this]
    {key (fn [req]
           {:status 200
            :body
            (let [model (dynamic-template-data (:template-model this) req)]
              (render-resource
               "templates/page.html.mustache"
               (assoc model
                 :content (html [:div.container
                                 [:div.page-header
                                  [:h1 label]
                                  [:p content]]]))))})})
  (routes [_] ["" {path key}])
  (uri-context [_] ""))

(defn new-example-page [& {:as opts}]
  (let [label (or (:label opts) "Page")
        sanitized (str/replace label #"\s+" "")]
    (->> opts
         (merge {:label label
                 :content "Blank"
                 :key (keyword (.toLowerCase sanitized))
                 :path (str "/" (.toLowerCase sanitized))})
         map->ExamplePage
         (<- (using [:template-model])))))
