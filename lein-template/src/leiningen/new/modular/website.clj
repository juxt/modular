(ns {{name}}.website
  (:require
   [clojure.pprint :refer (pprint)]
   [com.stuartsierra.component :as component]
   [modular.ring :refer (WebRequestHandler)]
   [modular.bidi :refer (as-request-handler)]
   [modular.web-template :refer (dynamic-template-data)]
   [hiccup.core :refer (html h)]
   [bidi.bidi :refer (path-for RouteProvider handler)]
   [bidi.ring :refer (redirect)]
   [clostache.parser :refer (render-resource)]))

(defn home-page
  "Define a Liberator resource map for the index (home) page of the website"
  [template-model]
  (fn [req]
    {:status 200
     :body
     (let [model (dynamic-template-data template-model req)]
       (render-resource
        "templates/page.html.mustache"
        (-> model
            (assoc :content (render-resource "templates/home.html.mustache" model)))))}))

;; Consider the component below. It is defined by defrecord.
;; It satisfies 2 protocols: bidi.RouteProvider and modular.ring.WebRequestHandler

;; To satisfy bidi.RoutepProvider a component must provide a routes
;; function, which returns a bidi route structure.

;; The use of the bidi 'handler' function is to allow looser coupling
;; between generated hyperlinks and the request handlers they route
;; to. Every handler can be targeted by using a keyword in bidi's
;; path-for function. This eliminates string-munging code that would
;; otherwise be written to form URIs, with the implicit coupling between
;; this logic and the route structure that would result. Note it is
;; idiomatic to use namespaced keywords so that there is less chance of
;; conflict with other keywords used by other components.

;; By also satisfying the WebRequestHandler protocol, this component can be
;; made a direct dependency of a Ring-compatible web server like
;; http-kit or Jetty. Whether this is the only web-serving component in
;; an application, or one of many (composed together by a router),
;; depends on the system's dependency map defined in system.clj

;; Finally there is the constructor, a function which creates an
;; instance of the component record.

(defrecord Website []
  RouteProvider
  (routes [_]
    ["/" {"index.html"
          (handler ::index (home-page (:template-model this)))

          "" (redirect ::index)}])

  WebRequestHandler
  (request-handler [this] (as-request-handler this)))

(defn new-website []
  ;; TODO Depending on the size of the template model, we may want to
  ;; limit the template-model to minium keys that are necessary, in the
  ;; case that a SystemDynamicTemplateModel instance is provided.
  (component/using
   (->Website)
   [:template-model]))
