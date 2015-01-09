;; Copyright © 2014 JUXT LTD.

(ns modular.less
  (:require
   [com.stuartsierra.component :refer (Lifecycle)]
   [schema.core :as s]
   [clj-less.less :refer (run-compiler)]
   [clojure.java.io :as io]
   [modular.bidi :refer (WebService)]
   [ring.util.response :refer (file-response)]))

(defrecord LessCompiler []
  Lifecycle
  (start [this]
    (run-compiler this)
    this)
  (stop [this] this))

(defn new-less-compiler [& {:as opts}]
  (->> opts
       (merge {:engine :nashorn
               :loader #(slurp "resources/less/" %)})
       (s/validate {:engine (s/enum :javascript :rhino :nashorn)
                    :loader (s/=> s/Str s/Str)
                    :source-path s/Str
                    :target-path s/Str})
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
      (if (>
           (apply max (conj (map #(.lastModified %) (.listFiles (io/file resource-dir))) 1))
           (.lastModified (io/file target-path)))
        (do
          (println "Compiling bootstrap less files")
          (run-compiler this))
        (println "No bootstrap less compilation necessary"))
      this))
  (stop [this] this)

  WebService
  (request-handlers [_]
    {::css (fn [_] (file-response target-path))})
  (routes [_]
    ["/" {"css/bootstrap.css" ::css}])
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
