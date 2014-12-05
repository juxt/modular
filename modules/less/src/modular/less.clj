;; Copyright © 2014 JUXT LTD.

(ns modular.less
  (:require
   [com.stuartsierra.component :refer (Lifecycle)]
   [schema.core :as s]
   [clj-less.less :refer (run-compiler)]
   [clojure.java.io :as io]))

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

(defrecord CustomBootstrapLessCompiler [version resource-dir]
  Lifecycle
  (start [this]
    (let [custom (let [fl (io/file resource-dir "custom-bootstrap.less")]
                   (when (and (.exists fl) (.isFile fl))
                     (println "custom bootstrap exists")
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
      (run-compiler this)
      this))
  (stop [this] this))

(defn new-bootstrap-less-compiler
  "A constructor returning a configured Less compiler for Twitter Bootstrap resources"
  [& {:keys [version] :or {version "3.3.0"} :as opts}]

  (assert (io/resource (format "META-INF/resources/webjars/bootstrap/%s/less/bootstrap.less" version))
          "Bootstrap resources not found on classpath")
  (->> opts
       (merge {:engine :nashorn
               :resource-dir "resources/less"
               :version version
               :target-path "target/less/bootstrap.less"})
       (s/validate {:engine (s/enum :javascript :rhino :nashorn)
                    :resource-dir s/Str
                    :version s/Str
                    :target-path s/Str})
       map->CustomBootstrapLessCompiler))
