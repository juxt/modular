;; Copyright © 2014 JUXT LTD.

(ns leiningen.new.modular
  (:refer-clojure :exclude (split read *data-readers*))
  (:require
   [leiningen.new.templates :refer [renderer sanitize year name-to-path ->files *dir*]]
   [leiningen.core.main :as main]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :refer (pprint)]
   [clojure.string :refer (split trim)]
   [clojure.set :as set]
   [stencil.core :as stencil]
   [clojure.tools.reader :refer (read *data-readers*)]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
   ))

(def render (renderer "modular"))

(defn ensure-map
  "Turn vector style into map style, if necessary. For example: [:a :b :c] -> {:a :a, :b :b, :c :c}"
  [x]
  (if (sequential? x)
    (apply zipmap (repeat 2 x))
    x))

(defn indent [indent s]
  (apply str
         (interpose "\n"
                    (for [line (line-seq (io/reader (java.io.StringReader. s)))]
                      (str (apply str (repeat indent " ")) line)
                      ))))

(defn load-edn-string [s]
  (binding [*data-readers* {'markdown (fn [x] x)}]
    (read
     (indexing-push-back-reader
      (java.io.PushbackReader. (java.io.CharArrayReader. (.toCharArray s)))))))

(defn generate-password []
  (apply str
       (shuffle (concat
                 (take 3 (repeatedly #(rand-nth (map char (range (int \A) (inc (int \Z)))))))
                 (take 3 (repeatedly #(rand-nth (map char (range (int \a) (inc (int \z)))))))
                 (take 1 (repeatedly #(rand-nth (map char (range (int \0) (inc (int \9)))))))))))

(defn modular
  "Create a new modular project - TODO documentation show go here which
  will be shown on 'lein new :show modular' , but will only appear when
  released."

  [name & args]

  (let [augment-by (->> args
                        (keep (partial re-matches #"\+(.*)") )
                        (map second)    ; take the grouping
                        (map #(split % #"/"))
                        (map (partial apply keyword))
                        set)

        diminish-by (->> args
                         (keep (partial re-matches #"\-(.*)") )
                         (map second)   ; take the grouping
                         (map #(split % #"/"))
                         (map (partial apply keyword))
                         set)

        select-assembly? (fn [{:keys [assembly default?]}]
                           (or (contains? augment-by assembly)
                               (and default?
                                    (not (contains? diminish-by assembly)))))

        manifest (load-edn-string
                  (stencil/render-string
                   (slurp (io/resource "manifest.edn"))
                   {:name name}))

        component-names (->> manifest :assemblies
                             (filter select-assembly?)
                             (mapcat :components) set)

        components (->> manifest
                        :components
                        (filter (comp component-names :component)))

        data {:name name
              :sanitized (name-to-path name)
              :snake-cased-name (clojure.string/replace name #"_" "-")

              ;; Probably need to be sorted and with some conflict
              ;; resolution warnings. Design decision is to generate
              ;; anyway (the developer can always delete and start
              ;; over). Also, developers can be given a warning (ala
              ;; pacman on Arch) to tell them to expect conflicts -
              ;; these can be resolved later
              :dependencies
              (->> components (mapcat :dependencies) distinct sort)

              :requires
              (reduce-kv
               (fn [a k v] (conj a {:namespace k
                                    :refers (apply str (interpose " " (distinct (map (comp clojure.core/name) v))))}))
               []
               (->> components
                    (mapcat (juxt :constructor :requires))
                    (remove nil?)
                    (group-by (comp symbol namespace))))

              :components
              (->>
               (for [c components
                     :let [ctr (:constructor c)]]
                 {:component (or (:key c) (:component c))
                  :constructor (symbol (clojure.core/name ctr))
                  :args (if (empty? (:args c)) ""
                            (str " " (apply pr-str (:args c))))})
               (sort-by :component))

              #_:modular-dir
              #_(str (System/getProperty "user.home") "/src/modular")

              #_:cylon-dir
              #_(str (System/getProperty "user.home") "/src/cylon")

              :dependency-map
              (->> manifest :assemblies
                   (filter select-assembly?)
                   (mapcat :dependency-map)
                   (group-by first)
                   (reduce-kv (fn [acc k v]
                                (assoc acc k
                                       (into {}
                                             (mapcat
                                              (comp ensure-map second) v))))
                              {})
                   pprint
                   with-out-str
                   trim
                   (indent 2)
                   )}]

    (main/info (format "Generating a new modular project named %s with options :-\n%s"
                       name
                       (->> manifest :assemblies
                            (filter select-assembly?)
                            (map :assembly)
                            (interpose "\n")
                            (apply str))))

    (->files data
             ["project.clj" (render "project.clj" data)]
             ["dev/dev.clj" (render "dev.clj" (assoc data "password" (format "\"%s\"" (generate-password))))]
             ["dev/user.clj" (render "user.clj" data)]

             ["dev/dev_components.clj" (render "dev_components.clj" data)]

             ["src/{{sanitized}}/main.clj" (render "main.clj" data)]

             ["src/{{sanitized}}/system.clj" (render "system.clj" data)]

             ["src/{{sanitized}}/website.clj" (render "website.clj" data)]
             ;; TODO Write website.clj in terms of boilerplate.clj, it is currently too 'standalone'
             ["src/{{sanitized}}/boilerplate.clj" (render "boilerplate.clj" data)]

             ["src/{{sanitized}}/example_page.clj" (render "example_page.clj" data)]

             ["src/{{sanitized}}/restricted_page.clj" (render "restricted_page.clj" data)]

             ["test/{{sanitized}}/website_tests.clj" (render "website_tests.clj" data)]

             ["src-cljs/{{sanitized}}/main.cljs" (render "main.cljs" data)]

             ;; Log configuration
             ["resources/logback.xml" (render "logback.xml" data)]

             ;; HTML
             ["resources/templates/page.html.mustache" (render "page.html.mustache")]
             ["resources/templates/home.html.mustache" (render "home.html.mustache")]

             ;; CSS
             ["resources/public/css/bootstrap.min.css" (render "resources/bootstrap.min.css")]
             ["resources/public/css/bootstrap-theme.min.css" (render "resources/bootstrap-theme.min.css")]

             ["resources/public/css/theme.css" (render "resources/theme.css")]

             ;; JS
             ["resources/public/js/bootstrap.min.js" (render "resources/bootstrap.min.js")]
             ["resources/public/js/jquery.min.js" (render "resources/jquery-2.1.1.min.js")]
             ["resources/public/js/jquery.min.map" (render "resources/jquery-2.1.1.min.map")]
             ["resources/public/js/react.js" (render "resources/react-0.9.0.js")]

             )))
