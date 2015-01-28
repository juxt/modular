(ns {{name}}.website
  (:require
   [bidi.bidi :refer (path-for alts)]
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
   [endophile.hiccup :refer (to-hiccup)]
   [hiccup.core :refer (html)]
   [schema.core :as s]))

(defn page [{:keys [templater router resources title]} content-template data req]
  (let [ ;; By getting the static context from the resources component,
        ;; we avoid hardcoding its uri-context
        ctx (path-for (:routes @router) (:target resources))]
    (response
     (render-template
      templater
      "templates/page.html.mustache"    ; our Mustache template
      {:brand-title title
       :title (:title data)
       :static ctx
       :links {:about (path-for (:routes @router) ::about)
               :contact (path-for (:routes @router) ::contact)}
       :home (path-for (:routes @router) ::index) ; href to home page
       :content (render-template templater
                                 (str "templates/" content-template)
                                 (assoc data :static ctx))}))))

(def META #"(\w+):\s+(.*)")

(defn extract-meta [s]
  (into {}
        (keep (fn [line]
                (when-let [[_ k v]
                           (re-matches META line)]
                  [(keyword (.toLowerCase k)) v]
                  )) s)))

(defn get-post [post routes]
  (let [doc (partition-by #(nil? (re-matches META %)) (line-seq (io/reader (io/file "posts" (str post ".md")))))
        [header body] (case (count doc)
                        1 [nil (first doc)]
                        2 doc
                        )]
    (merge (if header
             (extract-meta header)
             {:title "No title" :subtitle "No subtitle"})
           {:href (path-for routes ::post :post post)
            :body body})))

(defn get-posts [routes]
  (for [f (.listFiles (io/file "posts"))
        :let [name (second (re-matches #"(.*).md" (.getName f)))]]
    (get-post name routes)))

(defn index [this req]
  (page this "index.html.mustache"
        {:title (:title this)
         :subtitle (:subtitle this)
         :posts (get-posts (:routes @(:router this)))}
        req))

(defn post [this req]
  (page this "post.html.mustache"
        (update-in
         (get-post (-> req :route-params :post)
                   (:routes @(:router this))
                   )
         [:body]
         #(html (to-hiccup (mp (apply str (interpose \newline %)))))
         ) req))

(defrecord Website [title subtitle templater router resources cljs-builder]

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
  (routes [_] ["" [["/index.html" ::index]
                   [["/posts/" :post ".html"] ::post]
                   ["/about.html" ::about]
                   ["/contact.html" ::contact]
                   [(alts "/foo" "" "/") (redirect ::index)]]])

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
