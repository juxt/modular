(ns {{name}}.website
  (:require
   [clojure.pprint :refer (pprint)]
   [clojure.tools.logging :refer :all]
   [bidi.bidi :refer (RouteProvider tag)]
   [bidi.ring :refer (redirect)]
   [com.stuartsierra.component :refer (using)]
   [cylon.user.protocols :refer (LoginFormRenderer)]
   [cylon.session :refer (session)]
   [hiccup.core :refer (html)]
   [modular.bidi :refer (as-request-handler path-for)]
   [modular.component.co-dependency :refer (co-using)]
   [modular.template :as template :refer (render-template template-model TemplateModel)]
   [modular.template.util :refer (model->template-model)]))

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

(defn page [template templater *template-model session-store]
  (fn [req]
    (infof "session-store is %s" (session session-store req))
    {:status 200
     :body (page-body templater template
                      (merge
                       (template/template-model @*template-model req)
                       (when-let [s (session session-store req)]
                         {:user {:email (-> s :cylon/user :email)}})))}))

(defrecord Website [templater *template-model *router session-store]
  RouteProvider
  (routes [_]
    ["/" {"index.html"
          (-> (page "templates/index.html.mustache" templater *template-model session-store)
              (tag ::index))
          "protected.html"
          (-> (page "templates/protected.html.mustache" templater *template-model session-store)
              (tag ::protected))
          "" (redirect ::index)}])

  LoginFormRenderer
  (render-login-form [component req model]
    (page-body templater "templates/login.html.mustache"
               (merge (template/template-model @*template-model req)
                      #_(model->template-model model)
                      {:login-form
                       (html
                        [:form {:action (-> model :form :action)
                                :method (-> model :form :method)}
                         ;; Hidden fields
                         (for [{:keys [name value type]} (-> model :form :fields)
                               :when (= type "hidden")]
                           [:input {:type type :name name :value value}])
                         [:div
                          [:label {:for "email"} "Email"]
                          ;; We must have
                          [:input#email {:type :text :name "user"}]]
                         [:div
                          [:label {:for "password"} "Password"]
                          [:input#password {:type :password :name "password"}]
                          [:a {:href "#"} "Forgot password"]]
                         [:div
                          [:input.submit {:type "submit" :value "Sign in"}]
                          [:a {:href "#"} "Sign up"]]])})))

  TemplateModel
  (template-model [component req]
    (let [login-href (str (path-for @*router :cylon.user.login/login-form)
                          "?post_login_redirect="
                          (path-for @*router ::index))
          logout-href (str (path-for @*router :cylon.user.login/logout)
                           "?post_logout_redirect="
                           (path-for @*router ::index))]
      {:menu []
       :message "Hello!"
       :login-href login-href
       :logout-href logout-href
       })))

(defn new-website []
  (->
   (map->Website {})
   (using [:templater])
   (co-using [:router :template-model])))
