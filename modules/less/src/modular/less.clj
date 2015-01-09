;; Copyright © 2014 JUXT LTD.

(ns modular.less
  (:require
   [com.stuartsierra.component :refer (Lifecycle)]
   [schema.core :as s]
   [clj-less.less :refer (run-compiler)]
   [clojure.java.io :as io]
   [modular.bidi :refer (WebService)]
   [ring.util.response :refer (file-response content-type)]))

(defn source->target [path]
  (when-let [[_ s] (re-matches #"(.*)\.less" path)]
    (str s ".css")))

(defn stale? [source-dir target]
  (>
   (apply max (conj (map #(.lastModified %) (.listFiles (io/file source-dir))) 1))
   (.lastModified (io/file target))))

(defrecord LessCompiler [uri-context source-dir source-path target-dir target-path]
  Lifecycle
  (start [this]
    (let [target-path (or target-path (source->target source-path))
          target (io/file target-dir target-path)]
      (when (stale? source-dir target)
        (run-compiler (assoc this
                             :source-path (str source-path)
                             :target-path (str target)
                             )))
      (assoc this :target target :target-path target-path)))

  (stop [this] this)

  WebService
  (request-handlers [_] {})

  (routes [this]
    ["/" [[(:target-path this)
           (fn [_]
             (-> (:target this)
               str file-response
               (content-type "text/css")))]]])

  (uri-context [_] uri-context))

(def default-src-dir "src/less")

(defn new-less-compiler [& {:as opts}]
  (->> opts
    (merge
     {:engine :nashorn
      :source-dir default-src-dir
      :loader #(slurp (str (io/file (or (:source-dir opts) default-src-dir) %)))
      :target-dir "target/css"
      :uri-context "/css"})
    (s/validate
     {:engine (s/enum :javascript :rhino :nashorn)
      :loader (s/=> s/Str s/Str)
      :source-dir s/Str
      :source-path s/Str
      :target-dir s/Str
      ;; target-path, if missing, will be a result of concatenating the
      ;; target-dir with the source-path (with a css suffix)
      (s/optional-key :target-path) s/Str
      :uri-context s/Str})
    map->LessCompiler))

(defrecord CustomBootstrapLessCompiler [version resource-dir target-path]
  Lifecycle
  (start [this]
    (io/file resource-dir)
    (let [custom (let [fl (io/file resource-dir "custom-bootstrap.less")]
                   (when (and (.exists fl) (.isFile fl))
                     fl))
          this
          (cond->
           this
           true (assoc :source-path
                  (or custom
                      (str (io/resource (format "META-INF/resources/webjars/bootstrap/%s/less/bootstrap.less" version)))))
           custom (assoc :loader
                    (fn [x]
                      (println "Loading" x)
                      (slurp
                       (if-let [bootstrap-path (second (re-matches #"/bootstrap/(.*)" x))]
                         (let [res-path (format "META-INF/resources/webjars/bootstrap/%s/%s" version bootstrap-path)]
                           (io/resource res-path))
                         x)))))]
      (if (stale? resource-dir target-path)
        (do
          (println "Compiling bootstrap less files")
          (run-compiler this))
        (println "No bootstrap less compilation necessary"))
      this))
  (stop [this] this)

  WebService
  (request-handlers [_]
    {::bootstrap-css (fn [_] (file-response target-path))})
  (routes [_]
    ["/" {"css/bootstrap.css" ::bootstrap-css}])
  (uri-context [_]
    "/custom-bootstrap"))

(defn new-bootstrap-less-compiler
  "A constructor returning a configured Less compiler for Twitter Bootstrap resources"
  [& {:keys [version] :or {version "3.3.0"} :as opts}]

  (if-not (io/resource (format "META-INF/resources/webjars/bootstrap/%s/less/bootstrap.less" version))
    (throw
     (ex-info
      (format "Bootstrap resources not found on classpath, have you added [org.webjars/bootstrap \"%s\"] to project.clj?" version)
      {})))

  (->> opts
    (merge {:engine :nashorn
            :resource-dir "resources/less"
            :version version
            :target-path "target/css/bootstrap.css"})
    (s/validate {:engine (s/enum :javascript :rhino :nashorn)
                 :resource-dir s/Str
                 :version s/Str
                 :target-path s/Str})
    map->CustomBootstrapLessCompiler))
