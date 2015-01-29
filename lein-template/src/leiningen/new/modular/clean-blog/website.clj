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
   [endophile.core :refer (mp to-clj html-string)]
   [hiccup.core :refer (html)]
   [schema.core :as s]
   [clj-time.coerce :refer (to-long)]
   [clj-time.format :refer (parse unparse formatter)
                    :rename {parse parse-date
                             unparse unparse-date
                             formatter date-format}]))

(defn path-for [router target & args]
  (apply bidi/path-for (:routes @router) target args))

(defn static-path [router resources]
  ; ask bidi to determine the static context where public resources are mounted
  (path-for router (:target resources)))

(defn page [{:keys [templater router resources title]} content-template data req]
  (let [static (static-path router resources)]
    (response
     (render-template
      templater
      "templates/page.html.mustache" ; our Mustache template
      {:brand-title title
       :title (:title data)
       :static static
       :links {:about (path-for router ::about)}
       :home (path-for router ::index) ; href to home page
       :content (render-template templater
                                 (str "templates/" content-template)
                                 (assoc data :static static))}))))

(defn process-html [router resources s]
  (let [static (static-path router resources)]
    (clojure.walk/postwalk
     (fn [x]
       (cond
         (= :img (:tag x)) (update-in x [:attrs :src] #(str static %))
         :else x)) s)))

(defn format-date [s]
  (when s
    (unparse-date (date-format "EEEE, d MMMM, y") s)))

(defn get-post [post router resources]
  "Get the data associated with a post, including a delayed
  body (as :body). Router can be nil, for testing, but will result in
  nil hrefs being generated"
  (let [regex #"(\w+):\s+(.*)"
        extract-meta
        (fn [s]
          (let [res
                (->> s
                  (keep (fn [line]
                          (when-let [[_ k v]
                                     (re-matches regex line)]
                            (let [k (keyword (.toLowerCase k))]
                              [k ((case k :date parse-date identity) v)]))))
                  (into {}))]
            (pprint res)
            res))]
    (let [doc (group-by #(some? (re-matches regex %)) (line-seq (io/reader (io/file "posts" (str post ".md")))))]
      (let [{:keys [author date] :as meta} (extract-meta (get doc true))]
        (merge
         meta
         (when (or author date)
           {:attribution (format "Posted%s%s"
                                 (if author (format " by %s" author) "")
                                 (if date (format " on %s" (format-date date)) ""))})
         {:href (when router (path-for router ::post :post post))
          :body (->> (get doc false)
                  (interpose \newline)
                  (apply str) mp to-clj (process-html router resources) html-string delay)})))))

(defn get-posts [router resources]
  (sort-by (comp (fnil to-long 0) :date) >
           (for [f (.listFiles (io/file "posts"))
                 :let [post (second (re-matches #"(.*).md" (.getName f)))]]
             (get-post post router resources))))

(defn index [{:keys [title subtitle router resources] :as this} req]
  (page this "index.html.mustache"
        {:title title
         :subtitle subtitle
         :posts (get-posts router resources)}
        req))

(defn post [{:keys [router resources] :as this} req]
  (let [post (get-post (-> req :route-params :post)
                       router resources)]
    (page this "post.html.mustache"
          (-> post
            (update-in [:body] deref))
          req)))

(defrecord Website [title subtitle templater router resources cljs-builder]
  ;; modular.bidi provides a router which dispatches to routes provided
  ;; by components that satisfy its WebService protocol
  WebService
  (request-handlers [this]
    ;; Return a map between some keywords and their associated Ring
    ;; handlers
    {::index (fn [req] (index this req))
     ::about (fn [req] (page this "about.html.mustache" {} req))
     ::post (fn [req] (post this req))})

  ;; All paths lead to the dashboard
  (routes [_] ["" [["/index.html" ::index]
                   [["/posts/" :post ".html"] ::post]
                   ["/about.html" ::about]
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
