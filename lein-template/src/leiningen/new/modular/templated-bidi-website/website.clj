(ns {{name}}.website
  (:require
   [clojure.pprint :refer (pprint)]
   [modular.ring :refer (WebRequestHandler)]
   [modular.bidi :refer (as-request-handler)]
   [modular.template :as template :refer (render-template TemplateModel)]
   [bidi.bidi :refer (path-for RouteProvider tag)]
   [bidi.ring :refer (redirect)]
   [hiccup.core :refer (html)]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer (using)]))

(defn index
  "Render an index page, with the given templater and a template-model
  spanning potentially numerous records satisfying modular.template's
  TemplateModel protocol."
  [templater template-model]
  (fn [req]
    (infof "Rendering template")
    (debugf "Debug")
    {:status 200
     :body
     (render-template
      templater
      "templates/page.html.mustache"
      {:content
       (render-template
        templater
        "templates/content.html.mustache"
        (template/template-model template-model req))})}))

(defrecord Website [templater template-model]
  RouteProvider
  (routes [_]
    ["/" {"index.html" (-> (index templater template-model)
                           (tag ::index))
          "" (redirect ::index)}])

  WebRequestHandler
  (request-handler [this] (as-request-handler this)))

(defn new-website []
  (-> (map->Website {})
      (using [:templater :template-model])))

;; This record is one of the dependencies of template's
;; aggregate-template-model.
(defrecord ApplicationTemplateModel []
  TemplateModel
  (template-model [component req]
    {:message "Hello!"
     :user-agent (get-in req [:headers "user-agent"])}))

(defn new-application-template-model []
  (-> (map->ApplicationTemplateModel {})
      (using [])))
