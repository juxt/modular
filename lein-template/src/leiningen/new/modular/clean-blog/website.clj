(ns {{name}}.website
  (:require
   [bidi.bidi :refer (path-for)]
   [bidi.ring :refer (redirect)]
   [clojure.pprint :refer (pprint)]
   [clojure.tools.logging :refer :all]
   [clojure.java.io :as io]
   [com.stuartsierra.component :refer (using)]
   [modular.bidi :refer (WebService as-request-handler)]
   [modular.ring :refer (WebRequestHandler)]
   [modular.template :refer (render-template template-model)]
   [ring.util.response :refer (response)]
   [tangrammer.component.co-dependency :refer (co-using)]
   [endophile.core :refer (mp)]
   [endophile.hiccup :refer (to-hiccup)]))

(defn get-posts []
  (for [f (.listFiles (io/file "posts"))
        :let [content (to-hiccup (mp (slurp f)))]]
    {:title (-> content first second)
     :subtitle (-> content second second)
     :href (str "/posts/" (second (re-matches #"(.*)\.md" (.getName f))) ".html")}))

(defn get-post [post]
  (let [content (to-hiccup (mp (slurp (io/file "posts" (str post ".md")))))]
    {:title (-> content first second)
     :subtitle (-> content second second)}))

(defn page [{:keys [templater]} content-template data req]
  (response
   (render-template
    templater
    "templates/page.html.mustache" ; our Mustache template
    {:title "My Blog"
     :subtitle "Musings on adventures in the amazing world of Clojure"
     :content (render-template templater (str "templates/" content-template)
                               data)})))

;; Components are defined using defrecord.

(defn index [this req]
  (page this "index.html.mustache" {:posts (get-posts)} req))

(defn post [this req]
  (page this "post.html.mustache" (get-post (-> req :route-params :post)) req))

(defrecord Website [templater router cljs-builder]

  ; modular.bidi provides a router which dispatches to routes provided
  ; by components that satisfy its WebService protocol
  WebService
  (request-handlers [this]
    ;; Return a map between some keywords and their associated Ring
    ;; handlers
    {::index (fn [req] (index this req))
     ::about (fn [req] (page this "about.html.mustache" {} req))
     ::contact (fn [req] (page this "contact.html.mustache" {} req))
     ::post (fn [req] (post this req))})

  ;; All paths lead to the dashboard
  (routes [_] ["/" [["index.html" ::index]
                    [["posts/" :post ".html"] ::post]
                    ["about.html" ::about]
                    ["contact.html" ::contact]
                    ["" (redirect ::index)]]])

  ;; A WebService can be 'mounted' underneath a common uri context
  (uri-context [_] ""))

;; While not mandatory, it is common to use a function to construct an
;; instance of the component. This affords the opportunity to control
;; the construction with parameters, provide defaults and declare
;; dependency relationships with other components.

(defn new-website []
  (-> (map->Website {})
      (using [:templater])
      (co-using [:router])))
