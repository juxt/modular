(ns {{name}}.website
  (:require
   [bidi.bidi :refer (path-for RouteProvider handler)]
   [bidi.ring :refer (redirect)]
   [clojure.pprint :refer (pprint)]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer (using)]
   [hiccup.core :as hiccup]
   [modular.bidi :refer (as-request-handler)]
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

  ;; modular.bidi provides a router which dispatches to routes provided
  ;; by components that satisfy its RouteProvider protocol
  RouteProvider
  (routes [component]
    ;; All paths lead to the dashboard
    ["/" [["dashboard/"
           (handler ::dashboard (fn [req] (page component req)))]

          ;; You cannot redirect to a target that requires a parameter
          [["dashboard/" [#".*" :path]]
           (handler ::dashboard (fn [req] (page component req)))]

          ["" (redirect ::dashboard)]]]))

;; While not mandatory, it is common to use a function to construct an
;; instance of the component. This affords the opportunity to control
;; the construction with parameters, provide defaults and declare
;; dependency relationships with other components.

(defn new-website []
  (-> (map->Website {})
      (using [:templater :cljs-builder])
      (co-using [:router])))
