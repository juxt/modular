(ns {{name}}.pages
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :refer (pprint)]
   [clojure.string :as string]
   [clojure.tools.logging :refer :all]
   [bidi.bidi :as bidi :refer (tag RouteProvider)]
   [bidi.ring :refer (redirect)]
   [clj-time.coerce :refer (to-long)]
   [clj-time.format :refer (parse unparse formatter)
                    :rename {parse parse-date
                             unparse unparse-date
                             formatter date-format}]
   [com.stuartsierra.component :refer (using)]
   [endophile.core :refer (mp to-clj html-string)]
   [hiccup.core :refer (html)]
   [modular.bidi :refer (as-request-handler path-for)]
   [modular.component.co-dependency :refer (co-using)]
   [modular.ring :refer (WebRequestHandler)]
   [modular.template :refer (render-template template-model)]
   [ring.util.response :refer (response)]
   [schema.core :as s]
   ))

(defn page [{:keys [templater *router title]} content-template data req]
  (let [static (path-for @*router :web-resources)]
    (response
     (render-template
      templater
      "templates/page.html.mustache" ; our Mustache template
      {:brand-title title
       :title (:title data)
       :static static
       :links {:about (path-for @*router ::about)}
       :home (path-for @*router ::index) ; href to home page
       :content (render-template templater
                                 (str "templates/" content-template)
                                 (assoc data :static static))}))))

(defn process-html [*router s]
  (let [static (path-for @*router :web-resources)]
    (clojure.walk/postwalk
     (fn [x]
       (cond
         (= :img (:tag x)) (update-in x [:attrs :src] #(str static %))
         :else x)) s)))

(defn format-date [s]
  (when s
    (unparse-date (date-format "EEEE, d MMMM, y") s)))

(defn get-post [post *router]
  "Get the data associated with a post, including a delayed
  body (as :body). Router can be nil, for testing, but will result in
  nil hrefs being generated"
  (let [regex #"(\w+):\s+(.*)"
        extract-meta
        (fn [s]
          (->> s
            (keep (fn [line]
                    (when-let [[_ k v]
                               (re-matches regex line)]
                      (let [k (keyword (.toLowerCase k))]
                        [k ((case k :date parse-date identity) v)]))))
            (into {})))]
    (let [doc (group-by #(some? (re-matches regex %)) (line-seq (io/reader (io/file "posts" (str post ".md")))))]
      (let [{:keys [author date] :as meta} (extract-meta (get doc true))]
        (merge
         {:title "" :subtitle ""} ; blank out similar keys in main document
         meta
         (when (or author date)
           {:attribution (format "Posted%s%s"
                                 (if author (format " by %s" author) "")
                                 (if date (format " on %s" (format-date date)) ""))})
         {:href (when *router (path-for @*router ::post :post post))
          :body (->> (get doc false)
                  (interpose \newline)
                  (apply str) mp to-clj (process-html *router) html-string delay)})))))

(defn get-posts [*router]
  (sort-by (comp (fnil to-long 0) :date) >
           (for [f (.listFiles (io/file "posts"))
                 :let [post (second (re-matches #"(.*).md" (.getName f)))]]
             (get-post post *router))))

(defn index [{:keys [title subtitle *router] :as this} req]
  (page this "index.html.mustache"
        {:title title
         :subtitle subtitle
         :posts (get-posts *router)}
        req))

(defn post [{:keys [*router] :as this} req]
  (let [post (get-post (-> req :route-params :post) *router)]
    (page this "post.html.mustache"
          (-> post
            (update-in [:body] deref))
          req)))

(defrecord Pages [title subtitle templater *router cljs-builder]
  RouteProvider
  (routes [component]
    ["/myblog"
     [["/index.html"
       (-> (fn [req] (index component req))
           (tag ::index))]

      [["/posts/" :post ".html"]
       (-> (fn [req] (post component req))
           (tag ::post))]

      ["/about.html"
       (-> (fn [req] (page component "about.html.mustache" {} req))
           (tag ::about))]

      [(bidi/alts "" "/")
       (redirect ::index)]]]))

;; While not mandatory, it is common to use a function to construct an
;; instance of the component. This affords the opportunity to control
;; the construction with parameters, provide defaults and declare
;; dependency relationships with other components.

(defn new-pages [& {:as opts}]
  (-> (->> opts
        (merge {:title "default title here"
                :subtitle "default subtitle here"})
        (s/validate {:title s/Str
                     :subtitle s/Str})
        map->Pages)
    (using [:templater])
    (co-using [:router])))
