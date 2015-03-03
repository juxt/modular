(ns {{name}}.website
  (:require
   [clojure.pprint :refer (pprint)]
   [cylon.user.protocols :refer (LoginFormRenderer)]
   [modular.bidi :refer (as-request-handler)]
   [modular.template :as template :refer (render-template template-model TemplateModel)]
   [bidi.bidi :refer (RouteProvider tag)]
   [modular.bidi :refer (path-for)]
   [bidi.ring :refer (redirect)]
   [hiccup.core :refer (html)]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer (using)]
   [tangrammer.component.co-dependency :refer (co-using)]))

(defn page-body
  "Render a page body, with the given templater and a (deferred)
  template-model spanning potentially numerous records satisfying
  modular.template's TemplateModel protocol."
  [templater template model]
  (render-template
   templater
   "templates/page.html.mustache"
   (merge model
          {:content
           (render-template
            templater
            template
            model)})))

(defn index [templater template-model*]
  (fn [req]
    {:status 200
     :body (page-body templater "templates/index.html.mustache"
                      (template/template-model @template-model* req))}))

(defrecord Website [templater template-model router]
  RouteProvider
  (routes [_]
    ["/" {"index.html"
          (-> (index templater template-model)
              (tag ::index))
          "" (redirect ::index)}])

  LoginFormRenderer
  (render-login-form [component req model]
    (page-body templater "templates/login.html.mustache"
               (merge (template/template-model @template-model req)
                      {:login-form
                       (html
                        (list
                         [:h2 "(insert form here)"]
                         [:form
                          [:label "User"]
                          [:input {:type :text}]]))})))

  TemplateModel
  (template-model [component req]
    (let [login-href (str (path-for @router :cylon.user.login/login-form) "?post_login_redirect=/protected")]
      {:menu [{:label "Login" :href login-href}]
       :message "Hello!"
       :login-href login-href
       })))

(defn new-website []
  (->
   (map->Website {})
   (using [:templater])
   (co-using [:router :template-model])))
