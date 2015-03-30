(ns {{name}}.website
  (:require
   [clojure.pprint :refer (pprint)]
   [clojure.tools.logging :refer :all]
   [bidi.bidi :refer (RouteProvider tag)]
   [bidi.ring :refer (redirect)]
   [com.stuartsierra.component :refer (using)]
   [hiccup.core :refer (html)]
   [modular.bidi :refer (as-request-handler path-for)]
   [modular.component.co-dependency :refer (co-using)]
   [modular.template :as template :refer (render-template template-model TemplateModel)]

   [clojure.data.csv :refer (read-csv)]
   [clojure.java.io :as io]
   [cheshire.core :as json]
   ))

(defn page-body
  "Render a page body, with the given templater and a (deferred)
  template-model spanning potentially numerous records satisfying
  modular.template's TemplateModel protocol."
  [templater template model]
  (render-template
   templater
   "page.html.mustache"
   (merge model
          {:content
           (render-template
            templater
            template
            model)})))

(defn index [templater *template-model]
  (fn [req]
    {:status 200
     :body (page-body templater "index.html.mustache"
                      (template/template-model @*template-model req))}))

(defn content-page [templater *template-model content]
  (fn [req]
    {:status 200
     :body (render-template
            templater
            "page.html.mustache"
            (assoc @*template-model
                   :content content
                   ))}))

(defn exercise-csv [templater *template-model]
  (content-page
   templater *template-model
   (html
    [:h1 "Exercise 1"]
    ;; Write code for exercise 2 here
    )))

(defn exercise-json [templater *template-model]
  (content-page
   templater *template-model
   (html
    [:h1 "Exercise 2"]
    ;; Write code for exercise 2 here
    )))

(defrecord Website [templater *template-model *router]
  RouteProvider
  (routes [_]
    ["/" {"index.html"
          (-> (index templater *template-model)
              (tag ::index))
          "exercise-1.html" (exercise-csv templater *template-model)
          "exercise-2.html" (exercise-json templater *template-model)
          "" (redirect ::index)}])

  TemplateModel
  (template-model [component req]
    {:menu [{:label "Exercise 1" :href "exercise-1.html"}
            {:label "Exercise 2" :href "exercise-2.html"}]
     }))

(defn new-website []
  (->
   (map->Website {})
   (using [:templater])
   (co-using [:router :template-model])))
