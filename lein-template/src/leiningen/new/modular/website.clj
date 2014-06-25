(ns {{name}}.website
  (:require
   [clojure.pprint :refer (pprint)]
   [com.stuartsierra.component :as component]
   [modular.ring :refer (WebRequestHandler)]
   [modular.bidi :refer (WebService as-request-handler)]
   [hiccup.core :refer (html h)]
   [liberator.core :refer (resource)]
   [bidi.bidi :refer (path-for ->Redirect)]
   [clostache.parser :refer (render-resource)]))

{{! "We change the delimiters to avoid conflicting with nested destructuring in the code below" }}
{{=<% %>=}}

(defn index
  "Define a Liberator resource map for the index (home) page of the website"
  [template-model]
  {:available-media-types #{"text/html"}
   :handle-ok
   (fn [{{routes :modular.bidi/routes} ; it is idiomaic to destructure
                                       ; the bidi route structure from
                                       ; the request
         :request}]
<%#view-style.hiccup%>
     (html
      [:head
       [:title "Welcome"]]
      [:body
       [:h1 "Hello World! from <% name %>"]
       [:h2 "Links"]
       [:p [:a {:href (path-for routes ::index)} "Home"]]
       [:h2 "Template Model"]
       [:pre
        (h (with-out-str (pprint template-model)))
        ]
       ])
<%/view-style.hiccup%>
<%#view-style.mustache%>
      ;; TODO Mustache templating
      (render-resource "templates/page.html.mustache" template-model)
<%/view-style.mustache%>
     )})

;; Consider the component below. It is defined by defrecord.
;; It satisfies 2 protocols: modular.bidi.WebService and modular.ring.WebRequestHandler

;; To satisfy modular.bidi.WebSerivce a component must provide the following:
;;   request-handler-map: A map between keywords and Ring handlers
;;   routes: A bidi route structure, where terminals are keywords
;;              - these keywords correspond to the keys in request-handler-map
;;   uri-context: Usually an empty string, but acts as a prefix to the route structure

;; The use of keywords is to allow looser coupling between generated
;; hyperlinks and the request handlers they route to. Every handler can
;; be targeted by using a keyword in bidi's path-for function. This
;; eliminates string-munging code that would otherwise be written to
;; form URIs, with the implicit coupling between this logic and the
;; route structure that would result. Note it is idiomatic to use
;; namespaced keywords so that there is less chance of conflict with
;; other keywords used by other components.

;; By also satisfying the WebRequestHandler protocol, this component can be
;; made a direct dependency of a Ring-compatible web server like
;; http-kit or Jetty. Whether this is the only web-serving component in
;; an application, or one of many (composed together by a router),
;; depends on the system's dependency map defined in system.clj

;; Finally there is the constructor, a function which creates an
;; instance of the component record.

(defrecord Website []
  WebService
  (request-handlers [this]
    {::index (resource (index (:template-model this)))})

  (routes [_] ["/" {"index.html" ::index
                    "" (->Redirect 307 ::index)}])

  (uri-context [_] "")

  WebRequestHandler
  (request-handler [this] (as-request-handler this)))

(defn new-website []
  (component/using
   (->Website)
   [:template-model]))
