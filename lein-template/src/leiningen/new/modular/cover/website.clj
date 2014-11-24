(ns {{name}}.website
  (:require
   [clojure.pprint :refer (pprint)]
   [tangrammer.component.co-dependency :refer (co-using)]
   [com.stuartsierra.component :refer (using)]
   [modular.ring :refer (WebRequestHandler)]
   [modular.bidi :refer (WebService as-request-handler)]
   [modular.template :refer (render-template template-model)]
   [bidi.bidi :refer (path-for)]
   [bidi.ring :refer (redirect)]
   [hiccup.core :refer (html)]
   [clojure.tools.logging :refer :all]
   [ring.util.response :refer (response)]))

(defn page [templater router req content]
  (response
   (render-template
    templater
    "templates/page.html.mustache"
    {:menu
     (html
      [:ul.nav.masthead-nav
       (for [[k label] [[::index "Home"]
                        [::features "Features"]
                        [::about "About"]]
             ;; router is deref'd because it's a co-dependency
             :let [path (path-for (:routes @router) k)]]
         ;; TODO: class = active
         [:li (when (= path (:uri req)) {:class "active"})
          [:a (merge {:href path}) label]]
         )])
     :content content
     })))

(defn index [templater router]
  (fn [req]
    (page templater router req
          (html
           [:div
            [:h1.cover-heading "Welcome"]
            [:p.lead "Cover is a one-page template for
                  building simple and beautiful home pages. Download,
                  edit the text, and add your own fullscreen background
                  photo to make it your own."]
            ]))))

(defn features [templater router]
  (fn [req]
    (page templater router req
          (html
           [:div
            [:h1.cover-heading "Features"]

            ]))))

(defn about [templater router]
  (fn [req]
    (page templater router req
          (html
           [:div
            [:h1.cover-heading "About"]

            ]))))

(defrecord Website [templater router]
  WebService
  (request-handlers [this]
    {::index (index templater router)
     ::features (features templater router)
     ::about (about templater router)})

  (routes [_] ["/" {"index.html" ::index
                    "" (redirect ::index)
                    "features.html" ::features
                    "about.html" ::about}])

  (uri-context [_] "")

  WebRequestHandler
  (request-handler [this] (as-request-handler this)))

(defn new-website []
  (-> (map->Website {})
      (using [:templater])
      (co-using [:router])))
