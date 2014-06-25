;; Copyright © 2014 JUXT LTD.

(ns leiningen.new.modular
  (:require
   [leiningen.new.templates :refer [renderer sanitize year name-to-path ->files *dir*]]
   [leiningen.core.main :as main]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :refer (pprint)]
   [stencil.core :as stencil]))

(def render (renderer "modular"))

(defn ensure-map
  "Turn vector style into map style, if necessary. For example: [:a :b :c] -> {:a :a, :b :b, :c :c}"
  [x]
  (if (sequential? x)
    (apply zipmap (repeat 2 x))
    x))

(defn modular
  "Create a new modular project"
  [name]
  (let [manifest (edn/read-string
                  (stencil/render-string
                   (slurp (io/resource "manifest.edn"))
                   {:name name}))

        component-names (->> manifest :assemblies
                             (filter :default?)
                             (mapcat :components) set)

        components (->> manifest
                        :components
                        (filter (comp component-names :component)))

        data {:name name
              :sanitized (name-to-path name)
              :snake-cased-name (clojure.string/replace name #"_" "-")

              ;; Probably need to be sorted and with some conflict resolution warnings

              ;; Design decision is to generate anyway (the developer
              ;; can always delete and start over). Also, developers can
              ;; be given a warning (ala pacman on Arch) to tell them to
              ;; expect conflicts - these can be resolved later
              :dependencies
              (->> components (mapcat :dependencies) distinct)

              :requires
              (reduce-kv
               (fn [a k v] (conj a {:namespace k
                                    :refers (apply str (interpose " " (map (comp clojure.core/name) v)))}))
               []
               (->> components
                    (mapcat (juxt :constructor :requires))
                    (remove nil?)
                    (group-by (comp symbol namespace))))

              :components
              (for [c components
                    :let [ctr (:constructor c)]]
                {:component (or (:key c) (:component c))
                 :constructor (symbol (clojure.core/name ctr))
                 :args (if (empty? (:args c)) ""
                           (str " " (apply pr-str (:args c))))})

              :modular-dir
              (str (System/getProperty "user.home") "/src/modular")

              :cylon-dir
              (str (System/getProperty "user.home") "/src/cylon")

              ;; TODO Calculate view styles based on components present
              :view-style {:hiccup false
                           :mustache true}

              :dependency-map
              (->> manifest :assemblies
                   (filter :default?)
                   (mapcat :dependency-map)
                   (group-by first)
                   (reduce-kv (fn [acc k v]
                                (assoc acc k
                                       (into {}
                                             (mapcat
                                              (comp ensure-map second) v))))
                              {})
                   pprint
                   with-out-str)}]

    (main/info "Generating a new modular project named" (str name "..."))

    (->files data
             ["project.clj" (render "project.clj" data)]
             ["dev/dev.clj" (render "dev.clj" data)]
             ["dev/user.clj" (render "user.clj" data)]
             ["src/{{sanitized}}/main.clj" (render "main.clj" data)]

             ["src/{{sanitized}}/system.clj" (render "system.clj" data)]

             ["src/{{sanitized}}/website.clj" (render "website.clj" data)]
             ["test/{{sanitized}}/website_tests.clj" (render "website_tests.clj" data)]

             ["src-cljs/{{sanitized}}/main.cljs" (render "main.cljs" data)]

             ["resources/templates/page.html.mustache" (render "page.html.mustache")]

             ["resources/public/css/bootstrap.min.css" (render "resources/bootstrap.min.css")]
             ["resources/public/js/bootstrap.min.js" (render "resources/bootstrap.min.js")]
             ["resources/public/js/jquery.min.js" (render "resources/jquery.min.js")]
             ["resources/public/js/react.js" (render "resources/react-0.9.0.js")]

             )))
