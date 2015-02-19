(ns {{name}}.templated-bidi-website
  (:require
   [clojure.pprint :refer (pprint)]
   [modular.ring :refer (WebRequestHandler)]
   [modular.bidi :refer (as-request-handler)]
   [modular.template :refer (render-template template-model)]
   [bidi.bidi :refer (path-for RouteProvider handler)]
   [bidi.ring :refer (->Redirect)]
   [hiccup.core :refer (html)]
   [clojure.tools.logging :refer :all]))

(defn index [templater]
  (fn [req]
    (infof "Rendering template")
    (debugf "Debug")
    {:status 200
     :body
     (render-template
      templater
      "templates/page.html.mustache"
      {:content
       (html [:div.container
              [:div.page-header
               [:h1 "Welcome"]
               [:p "This text can be found in " [:code '{{name}}.templated-bidi-website]]]])})}))


(defrecord Website [templater]
  RouteProvider
  (routes [_]
    ["/" {"index.html" (handler ::index (index templater))
          "" (->Redirect 307 ::index)}])

  WebRequestHandler
  (request-handler [this] (as-request-handler this)))

(defn new-website []
  (map->Website {}))
