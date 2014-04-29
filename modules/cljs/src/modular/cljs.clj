;; Copyright © 2014 JUXT LTD.

(ns modular.cljs
  (:require
   [com.stuartsierra.component :as component]
   [modular.bidi :refer (WebService)]
   [modular.template :refer (TemplateModel)]
   [bidi.bidi :refer (->Files)]
   [shadow.cljs.build :as cljs]
   [clojure.java.io :as io]
   [schema.core :as s]
   [com.stuartsierra.dependency :as dep]))

(def cljs-compiler-state (ref {}))

(def cljs-compiler-compile-count (ref {}))

(defn add-modules [state modules]
  (reduce (fn [state {:keys [name mains dependencies]}]
            (cljs/step-configure-module state name mains dependencies))
          state modules))

(defn compile-cljs [builder modules {:keys [context source-path target-dir work-dir
                                            optimizations pretty-print]}]

  (let [state (if-let [s (get @cljs-compiler-state builder)]
                (do
                  (println "cljs: Using existing state")
                  s)
                (do
                  (println "cljs: Creating new state")
                  (-> (cljs/init-state)
                      (cljs/enable-source-maps)
                      (assoc :optimizations optimizations
                             :pretty-print pretty-print
                             :work-dir (io/file work-dir) ;; temporary output path, not really needed
                             :public-dir (io/file target-dir) ;; where should the output go
                             :public-path context) ;; whats the path the html has to use to get the js?
                      (cljs/step-find-resources-in-jars) ;; finds cljs,js in jars from the classpath
                      (cljs/step-find-resources "lib/js-closure" {:reloadable false})
                      (cljs/step-find-resources source-path) ;; find cljs in this path
                      (cljs/step-finalize-config) ;; shouldn't be needed but is at the moment
                      (cljs/step-compile-core)    ;; compile cljs.core
                      (add-modules modules)
                      )))]

    (let [new-state (-> state
                        (cljs/step-reload-modified)
                        (cljs/step-compile-modules)
                        (cljs/flush-unoptimized))]
      (dosync
       (alter cljs-compiler-state (fn [m] (assoc-in m [builder] new-state)))
       (alter cljs-compiler-compile-count (fn [m] (update-in m [builder] (fnil inc 0))))))

    (println (format "Compiled %d times since last full compile" (get @cljs-compiler-compile-count builder))))

  :done)

(defprotocol ClojureScriptModule
  ;; Return a map of :name, :mains and :dependencies
  (get-definition [_]))

(defn new-cljs-module [& {:as opts}]
  (let [opts
        (as-> opts %
              (merge {:dependencies #{}} %)
              (s/validate {:name s/Keyword
                           :mains [s/Any]
                           :dependencies #{s/Keyword}} %))]
    (reify ClojureScriptModule
      (get-definition [_] opts))))

(defprotocol JavaScripts
  (get-javascript-paths [_]))

(def new-cljs-builder-schema
  {:id s/Keyword
   :context s/Str
   :source-path s/Str
   :target-dir s/Str
   :work-dir s/Str
   :optimizations (s/enum :none :whitespace :simple :advanced)
   :pretty-print s/Bool
   })

(defrecord ClojureScriptBuilder []
  component/Lifecycle
  (start [this]
    (let [modules (map get-definition (filter (partial satisfies? ClojureScriptModule) (vals this)))]
      (try
        (compile-cljs (:id this)
                      modules (select-keys this (keys new-cljs-builder-schema)))
        (cond
         (and (= (:optimizations this) :none)
              (= (:pretty-print this) true))
         ;; Only do this on optimizations: none and pretty-print: true - do
         ;; something different for each optimization mode (TODO)
         (assoc this
           :javascripts
           (for [n (dep/topo-sort
                    ;; Build a dependency graph between all the modules so
                    ;; they load in the correct order.
                    (reduce (fn [g {:keys [name dependencies]}]
                              (reduce (fn [g d] (dep/depend g name d)) g dependencies))
                            (dep/graph) modules))]
             (str (:context this) (name n) ".js")))
         :otherwise this)
        (catch Exception e
          (println "ClojureScript build failed:" e)
          (assoc this :error e)))))
  (stop [this] this)

  WebService
  (ring-handler-map [this] {})
  (routes [this] ["" (->Files {:dir (:target-dir this)
                               :mime-types {"map" "application/javascript"}})])
  (uri-context [this] (:context this))

  JavaScripts
  (get-javascript-paths [this] (:javascripts this))

  TemplateModel
  (template-model [this _] {:javascripts (get-javascript-paths this)}))

(defn new-cljs-builder [& {:as opts}]
  (->> opts
        (merge {:id ::default
                :context "/cljs/"
                :source-path "src-cljs"
                :target-dir "target/cljs"
                :work-dir "target/cljs-work"
                :optimizations :none
                :pretty-print true})
        (s/validate new-cljs-builder-schema)
        map->ClojureScriptBuilder))
