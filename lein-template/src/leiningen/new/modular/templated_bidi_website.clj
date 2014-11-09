(ns {{name}}.templated-bidi-website
  (:require
   [clojure.pprint :refer (pprint)]
   [modular.ring :refer (WebRequestHandler)]
   [modular.bidi :refer (WebService as-request-handler)]
   [modular.template :refer (render-template template-model)]
   [bidi.bidi :refer (path-for ->Redirect)]
   [hiccup.core :refer (html)]
   [clojure.tools.logging :refer :all]))

(defrecord Website [aggregate-template-model templater]
  WebService
  (request-handlers [this]
    {::index (fn [req]
               (infof "Rendering template")
               (debugf "Debug")
               #_(template-model aggregate-template-model req)
               {:status 200
                :body
                (render-template
                 templater
                 "templates/page.html.mustache"
                 {:content
                  (html [:div.container
                         [:div.page-header
                          [:h1 "Welcome"]
                          [:p "This text can be found in " [:code '{{name}}.templated-bidi-website]]]])})})})

  (routes [_] ["/" {"index.html" ::index
                    "" (->Redirect 307 ::index)}])

  (uri-context [_] "")

  WebRequestHandler
  (request-handler [this] (as-request-handler this)))

(defn new-website []
  (map->Website {}))
