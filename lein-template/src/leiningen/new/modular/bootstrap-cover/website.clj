(ns {{name}}.website
  (:require
   [clojure.pprint :refer (pprint)]
   [clojure.tools.logging :refer :all]
   [bidi.bidi :refer (tag RouteProvider)]
   [bidi.ring :refer (redirect)]
   [com.stuartsierra.component :refer (using)]
   [hiccup.core :as hiccup]
   [modular.bidi :refer (as-request-handler path-for)]
   [modular.component.co-dependency :refer (co-using)]
   [modular.ring :refer (WebRequestHandler)]
   [modular.template :refer (render-template template-model)]
   [ring.util.response :refer (response)]))

(defn menu [*router uri]
  (hiccup/html
   [:ul.nav.masthead-nav
    (for [[k label] [[::index "Home"]
                     [::features "Features"]
                     [::about "About"]]
          ;; This demonstrates the generation of hyperlinks from
          ;; keywords.

          ;; by the way, router is deref'd because it's a
          ;; co-dependency, this is likely to change to potemkin's
          ;; def-map-type in future releases, so a deref will be
          ;; unnecessary (and deprecated)

          :let [href (path-for @*router k)]]
      [:li (when (= href uri) {:class "active"})
       [:a (merge {:href href}) label]]
      )]))

(defn page [templater *router req content]
  (response
   (render-template
    templater
    "templates/page.html.mustache" ; our Mustache template
    {:menu (menu *router (:uri req))
     :content content})))

(defn index [templater *router]
  (fn [req]
    (page templater *router req
          (hiccup/html
           [:div
            [:h1.cover-heading "Welcome"]
            [:p.lead "Cover is a one-page template for
                  building simple and beautiful home pages. Download,
                  edit the text, and add your own fullscreen background
                  photo to make it your own."]
            [:p "This is a Clojure project called {{name}}, generated
            from modular's bootstrap-cover template. This text can be
            found in " [:code "{{name}}/website.clj"]] ]))))

(defn features [templater *router]
  (fn [req]
    (page templater *router req
          (hiccup/html
           [:div
            [:h1.cover-heading "Features"]
            [:p.lead "bootstrap-cover exhibits the following :-"]
            [:ul.list-unstyled.lead
             [:li "A working Clojure-powered website using Stuart Sierra's 'reloaded' workflow and component library"]
             [:li "A fully-commented route-contributing website component"]
             [:li [:a {:href "https://github.com/juxt/bidi"} "Bidi"] " routing"]
             [:li "Co-dependencies"]
             [:li "Deployable with lein run"]
             ]
            [:p "This list can be found in " [:code "{{name}}/website.clj"]]]))))

(defn about [templater *router]
  (fn [req]
    (page templater *router req
          (hiccup/html
           [:div
            [:h1.cover-heading "About"]
            [:p.lead "You should
            edit " [:code "{{name}}/website.clj"] ", locate
            the " [:code "about"] " function and edit the function
            defintion to display your details here, describing who you are
            and why you started this project."]]))))

;; Components are defined using defrecord.

(defrecord Website [templater *router]

  ;; modular.bidi provides a router which dispatches to routes provided
  ;; by components that satisfy bidi's RouteProvider protocol
  RouteProvider

  ;; Return a bidi route structure, using bidi's tag wrapper to
  ;; assign keywords to Ring handlers, keywords that can be used in
  ;; calls to bidi's path-for function.
  (routes [_]
    ["/"
     {"index.html" (-> (index templater *router) (tag ::index))
      "" (redirect ::index)
      "features.html" (-> (features templater *router) (tag ::features))
      "about.html" (-> (about templater *router) (tag ::about))}]))

;; While not mandatory, it is common to use a function to construct an
;; instance of the component. This affords the opportunity to control
;; the construction with parameters, provide defaults and declare
;; dependency relationships with other components.

(defn new-website []
  (-> (map->Website {})
      (using [:templater])
      (co-using [:router])))
