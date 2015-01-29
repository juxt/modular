(ns {{name}}.website
  (:require
   [bidi.bidi :as bidi]
   [bidi.ring :refer (redirect)]
   [clojure.pprint :refer (pprint)]
   [clojure.tools.logging :refer :all]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [com.stuartsierra.component :refer (using)]
   [modular.bidi :refer (WebService as-request-handler)]
   [modular.ring :refer (WebRequestHandler)]
   [modular.template :refer (render-template template-model)]
   [ring.util.response :refer (response)]
   [tangrammer.component.co-dependency :refer (co-using)]
   [endophile.core :refer (mp)]
   [endophile.hiccup :refer (to-hiccup)]
   [hiccup.core :refer (html)]
   [schema.core :as s]))

(defn path-for [router target & args]
  (apply bidi/path-for (:routes @router) target args))

(defn page [{:keys [templater router resources title]} content-template data req]
  (let [static ; ask bidi to determine the static context where public resources are mounted
        (path-for router (:target resources))]
    (response
     (render-template
      templater
      "templates/page.html.mustache" ; our Mustache template
      {:brand-title title
       :title (:title data)
       :static static
       :links {:about (path-for router ::about)
               :contact (path-for router ::contact)}
       :home (path-for router ::index) ; href to home page
       :content (render-template templater
                                 (str "templates/" content-template)
                                 (assoc data :static static))}))))

(defn get-post [post router]
  (let [regex #"(\w+):\s+(.*)"
        extract-meta (fn [s]
                       (into {}
                             (keep (fn [line]
                                     (when-let [[_ k v]
                                                (re-matches regex line)]
                                       [(keyword (.toLowerCase k)) v]))
                                   s)))]
    (let [doc (group-by #(some? (re-matches regex %)) (line-seq (io/reader (io/file "posts" (str post ".md")))))]
      (merge
       (extract-meta (get doc true))
       {:href (path-for router ::post :post post)
        :body (->> (get doc false)
                (interpose \newline)
                (apply str) mp to-hiccup html delay)}))))

(defn get-posts [router]
  (for [f (.listFiles (io/file "posts"))
        :let [name (second (re-matches #"(.*).md" (.getName f)))]]
    (get-post name router)))

(defn index [this req]
  (page this "index.html.mustache"
        {:title (:title this)
         :subtitle (:subtitle this)
         :posts (get-posts (:router this))}
        req))

(defn post [this req]
  (page this "post.html.mustache"
        (-> (get-post (-> req :route-params :post)
                      (:router this))
          (update-in [:body] deref))
        req))

(defrecord Website [title subtitle templater router resources cljs-builder]

  ;; modular.bidi provides a router which dispatches to routes provided
  ;; by components that satisfy its WebService protocol

  WebService
  (request-handlers [this]
    ;; Return a map between some keywords and their associated Ring
    ;; handlers
    {::index (fn [req] (index this req))
     ::about (fn [req] (page this "about.html.mustache" {} req))
     ::contact (fn [req] (page this "contact.html.mustache" {} req))
     ::post (fn [req] (post this req))})

  ;; All paths lead to the dashboard
  (routes [_] ["" [["/index.html" ::index]
                   [["/posts/" :post ".html"] ::post]
                   ["/about.html" ::about]
                   ["/contact.html" ::contact]
                   [(bidi/alts "/foo" "" "/") (redirect ::index)]]])

  ;; A WebService can be 'mounted' underneath a common uri context
  (uri-context [_] "/myblog"))

;; While not mandatory, it is common to use a function to construct an
;; instance of the component. This affords the opportunity to control
;; the construction with parameters, provide defaults and declare
;; dependency relationships with other components.

(defn new-website [& {:as opts}]
  (-> (->> opts
        (merge {:title "default title here"
                :subtitle "default subtitle here"})
        (s/validate {:title s/Str
                     :subtitle s/Str})
        map->Website)
    (using [:templater :resources])
    (co-using [:router])))
