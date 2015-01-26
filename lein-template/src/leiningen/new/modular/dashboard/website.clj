(ns {{name}}.website
  (:require
   [bidi.bidi :refer (path-for)]
   [bidi.ring :refer (redirect)]
   [clojure.pprint :refer (pprint)]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer (using)]
   [hiccup.core :as hiccup]
   [modular.bidi :refer (WebService as-request-handler)]
   [modular.ring :refer (WebRequestHandler)]
   [modular.template :refer (render-template template-model)]
   [modular.cljs :refer (get-javascript-paths)]
   [ring.util.response :refer (response)]
   [tangrammer.component.co-dependency :refer (co-using)]))

(defn page [{:keys [templater router cljs-builder]} req]
  (response
   (render-template
    templater
    "templates/dashboard.html.mustache" ; our Mustache template
    {:javascripts (get-javascript-paths cljs-builder)})))

;; Components are defined using defrecord.

(defrecord Website [templater router cljs-builder]

  ; modular.bidi provides a router which dispatches to routes provided
  ; by components that satisfy its WebService protocol
  WebService
  (request-handlers [this]
    ;; Return a map between some keywords and their associated Ring
    ;; handlers
    {::dashboard (fn [req] (page this req))})

  ;; All paths lead to the dashboard
  (routes [_] ["/" [["dashboard/" ::dashboard]
                    ;; You cannot redirect to a target that requires a parameter
                    [["dashboard/" [#".*" :path]] ::dashboard]
                    ["" (redirect ::dashboard)]]])

  ;; A WebService can be 'mounted' underneath a common uri context
  (uri-context [_] ""))

;; While not mandatory, it is common to use a function to construct an
;; instance of the component. This affords the opportunity to control
;; the construction with parameters, provide defaults and declare
;; dependency relationships with other components.

(defn new-website []
  (-> (map->Website {})
      (using [:templater :cljs-builder])
      (co-using [:router])))
