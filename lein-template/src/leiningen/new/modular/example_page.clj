(ns {{name}}.example-page
  (:require
   [clojure.string :as str]
   [com.stuartsierra.component :refer (using)]
   [modular.menu :refer (MenuItems)]
   [modular.bidi :refer (WebService)]
   [modular.web-template :refer (dynamic-template-data)]
   [clostache.parser :refer (render-resource)]
   [plumbing.core :refer (<-)]
   [schema.core :as s]
   [hiccup.core :refer (html)]))

(defrecord ExamplePage [title content key path]
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
                                  [:h1 title]
                                  [:p content]]]))))})})
  (routes [_] ["" {path key}])
  (uri-context [_] ""))

(defn new-example-page [& {:as opts}]
  (->> opts
       (merge {:content "Blank"})
       (s/validate {:title s/Str
                    :content s/Str
                    :key s/Keyword
                    :path s/Str})
       map->ExamplePage
       (<- (using [:template-model]))))

(defrecord ExamplePageMenuItems [label target order]
  MenuItems
  (menu-items [_] [{:label label :target target :order order}]))

(defn new-example-page-menu-items [& {:as opts}]
  (->> opts
       (s/validate {:label s/Str
                    :target s/Keyword})
       map->ExamplePageMenuItems))
