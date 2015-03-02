(ns {{name}}.website
  (:require
   [clojure.pprint :refer (pprint)]
   [cylon.user.protocols :refer (LoginFormRenderer)]
   #_[modular.ring :refer (WebRequestHandler)]
   [modular.bidi :refer (as-request-handler)]
   [modular.template :as template :refer (render-template template-model TemplateModel)]
   [bidi.bidi :refer (RouteProvider tag)]
   [modular.bidi :refer (path-for)]
   [bidi.ring :refer (redirect)]
   [hiccup.core :refer (html)]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer (using)]
   [tangrammer.component.co-dependency :refer (co-using)]))

(defn index
  "Render an index page, with the given templater and a template-model
  spanning potentially numerous records satisfying modular.template's
  TemplateModel protocol."
  [templater template-model]
  (fn [req]
    (let [model (template/template-model template-model req)]
      {:status 200
       :body
       (render-template
        templater
        "templates/page.html.mustache"
        (merge model
               {:content
                (render-template
                 templater
                 "templates/content.html.mustache"
                 model)}))})))

(defrecord Website [templater template-model]
  RouteProvider
  (routes [_]
    ["/" {"index.html" (-> (index templater template-model)
                           (tag ::index))
          "" (redirect ::index)}])

  #_WebRequestHandler
  #_(request-handler [this] (as-request-handler this))

  LoginFormRenderer
  (render-login-form [component req model]
    "(login form here)"
    ))

(defn new-website []
  (->
   (map->Website {})
   (using [:templater :template-model])
   (co-using [:router])))

;; This record is one of the dependencies of template's
;; aggregate-template-model.
(defrecord ApplicationTemplateModel [router]
  TemplateModel
  (template-model [component req]
    {:menu [{:label "Login" :href (path-for @router :cylon.user.login/login-form)}]
     :message "Hello!"
     }))

(defn new-application-template-model []
  (-> (map->ApplicationTemplateModel {})
      (co-using [:router])))
