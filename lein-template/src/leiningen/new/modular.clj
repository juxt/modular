;; Copyright © 2014 JUXT LTD.

(ns leiningen.new.modular
  (:refer-clojure :exclude (split read *data-readers* replace))
  (:require
   [leiningen.new.templates :refer [renderer sanitize year name-to-path ->files *dir* *force?*]]
   [leiningen.core.main :as main]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :refer (pprint)]
   [clojure.string :refer (split trim replace)]
   [clojure.set :as set]
   [stencil.core :as stencil]
   [clojure.tools.reader :refer (read *data-readers*)]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
   ))

(def render (renderer "modular"))

(def KEY-TYPE :keyword)
;; An attempt is made to ensure keywords are unique by lengthening
;; them. But keyword pairs can be just as effective, and more useful
;; since they code the assembly keyword in a data-structure rather than
;; encoding as a string
#_(def KEY-TYPE :vector)

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

(defn close-parens [s]
  (replace s #"\n\s*\)" ")"))

(defn make-key [ns n]
  (case KEY-TYPE
    :vector [ns n]
    :keyword (keyword (apply str (interpose "-" (map #(if % (clojure.core/name %) nil) [ns n]))))))

(defn gbf
  "Group by first and apply function to values"
  [f seq] (reduce-kv
           (fn [a k v] (conj a [k (f (map second v))]))
           []
           (group-by first seq)))

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
                   {:name name
                    :dev-password (format "\"%s\"" (generate-password))}))

        component-keys (->> manifest :assemblies
                            (filter select-assembly?)
                            (mapcat :components) vals set)

        components (->> manifest
                        :components
                        (filter (comp component-keys :component)))

        components-by-id (->> manifest
                              :components
                              (map (juxt :component identity))
                              (into {}))

        assemblies (for [a (->> manifest :assemblies (filter select-assembly?))]
                     {:fname (str (clojure.core/name (:assembly a)) "-components")

                      :assembly (:assembly a)

                      :components
                      (for [[n {component-ref :component
                                using :using
                                dependencies :dependencies
                                args :args :as instance
                                }]
                            (:components a)
                            :let [component (when component-ref
                                              (get components-by-id component-ref))
                                  constructor (if component
                                                (:constructor component)
                                                (:constructor instance))]]
                        (merge
                         (when component
                           {:component component})

                         {:dependencies
                          (if component
                            (concat
                             (:dependencies (get components-by-id component-ref))
                             dependencies)
                            dependencies)}

                         {:key (make-key (:assembly a) n)
                          :refers (conj (:refers instance) constructor)
                          :constructor (symbol (clojure.core/name constructor))
                          :args (map (partial zipmap [:k :v]) (partition 2 args))

                          ;; Construct 'using' here, not in template
                          ;; it could be a map, it could be a vector

                          :using (pr-str
                                  (or (:using instance) []))



                          :pad10 (apply str (repeat (+ 10 (count (str n))) \space))
                          :pad18 (apply str (repeat (+ 18 (count (str n))) \space))
                          }))

                      :dependency-map (:dependency-map a)
                      })

        data {:name name
              :sanitized (name-to-path name)
              :snake-cased-name (clojure.string/replace name #"_" "-")

              #_:requires
              #_(reduce-kv
                 (fn [a k v] (conj a {:namespace k
                                      :refers (apply str (interpose " " (distinct (map (comp clojure.core/name) v))))}))
                 []
                 (->> components
                      (filter (comp not :dev?))
                      (mapcat (juxt :constructor :requires))
                      (remove nil?)
                      (group-by (comp symbol namespace))))

              #_:dev-requires
              #_(reduce-kv
                 (fn [a k v] (conj a {:namespace k
                                      :refers (apply str (interpose " " (distinct (map (comp clojure.core/name) v))))}))
                 []
                 (->> components
                      (filter :dev?)
                      (mapcat (juxt :constructor :requires))
                      (remove nil?)
                      (group-by (comp symbol namespace))))

              :assemblies assemblies

              ;; Probably need to be sorted and with some conflict
              ;; resolution warnings. Design decision is to generate
              ;; anyway (the developer can always delete and start
              ;; over). Also, developers can be given a warning (ala
              ;; pacman on Arch) to tell them to expect conflicts -
              ;; these can be resolved later
              :dependencies
              (->>
               (for [asmbly assemblies
                     c (:components asmbly)
                     dep (:dependencies c)]
                 dep)
               sort distinct)


              :refers
              (->>
               (for [asmbly assemblies
                     c (:components asmbly)
                     refer (:refers c)]
                 refer)
               sort distinct
               (group-by (comp symbol namespace))
               (reduce-kv
                (fn [a k v] (conj a {:namespace k
                                     :refers (apply str (interpose " " (distinct (map (comp clojure.core/name) v))))}))
                []
                )
               )

              #_:components
              #_(->>
                 (for [c components
                       :when (not (:dev? c))
                       :let [ctr (:constructor c)]]
                   {:component (or (:key c) (:component c))
                    :constructor (symbol (clojure.core/name ctr))
                    :args (if (empty? (:args c)) ""
                              (str " " (apply pr-str (:args c))))})
                 (sort-by :component))

              #_:dev-components
              #_(->>
                 (for [c components
                       :when (:dev? c)
                       :let [ctr (:constructor c)]]
                   {:component (or (:key c) (:component c))
                    :constructor (symbol (clojure.core/name ctr))
                    :args (if (empty? (:args c)) ""
                              (str " " (apply pr-str (:args c))))})
                 (sort-by :component))

              ;; Dependency maps will be useful for adding dependencies
              ;; to components that already exist, such as template
              ;; models and menus

              :dependency-map
              (pr-str
               (->>
                (for [a assemblies
                      :let [mkey #(if (keyword? %) (make-key (:assembly a) %)
                                      (apply make-key %))]

                      [k v] (:dependency-map a)
                      [n v] (ensure-map v)
                      ]
                  [(mkey k) [n (mkey v)]])
                ;; This call to 'first' should actually check to ensure
                ;; there aren't multiple entries, if there are it means
                ;; we have a conflict - more than one dependency is trying to bind
                (gbf #(into {} (gbf first %)))
                (into {})
                ))

              #_(->> assemblies
                     (mapcat :dependency-map)
                     (group-by first)
                     (reduce-kv (fn [acc k v]
                                  (assoc acc k
                                         (into {}
                                               (mapcat
                                                (comp ensure-map second) v))))
                                {})
                     #_pprint
                     #_with-out-str
                     #_trim
                     #_(indent 2)
                     )}]

    #_(println (->> assemblies
                    (mapcat :dependency-map)
                    (group-by first)
                    (reduce-kv (fn [acc k v]
                                 (assoc acc k
                                        (into {}
                                              (mapcat
                                               (comp ensure-map second) v))))
                               {})

                    ))

    (main/info (format "Generating a new modular project named %s with options :-\n%s"
                       name
                       (->> manifest :assemblies
                            (filter select-assembly?)
                            (map :assembly)
                            (interpose "\n")
                            (apply str))))

    ;;    (throw (ex-info "refers" {:refers (:refers data)}))

    (->files data
             ["project.clj" (render "project.clj" data)]
             ["dev/dev.clj" (render "dev.clj" data)]
             ["dev/user.clj" (render "user.clj" data)]

             ["dev/dev_components.clj" (render "dev_components.clj" data)]

             ["src/{{sanitized}}/main.clj" (render "main.clj" data)]

             ["src/{{sanitized}}/system.clj" (close-parens (render "system.clj" data))]

             ["src/{{sanitized}}/website.clj" (render "website.clj" data)]

             ;; Our Hello World! handler
             ["src/{{sanitized}}/simple_webservice.clj" (render "simple_webservice.clj" data)]

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
