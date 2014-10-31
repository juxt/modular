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
   [clojure.java.shell :refer (sh)]
   [stencil.core :as stencil]
   [clojure.tools.reader :refer (read *data-readers*)]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
   [clojure.tools.logging :refer :all]
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

  [name app-template & args]

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



        manifest (load-edn-string
                  (stencil/render-string
                   (slurp (io/resource "manifest.edn"))
                   {:name name
                    :dev-password (format "\"%s\"" (generate-password))
                    :sanitized (name-to-path name)}))

        select-assembly?
        (fn [{:keys [assembly]}]
          (when-let [includes
                     (conj (-> manifest :application-templates (get app-template #{})) :core)]
            (infof "select asm? %s %s" includes assembly)
            (or (contains? augment-by assembly)
                (and (includes assembly)
                     (not (contains? diminish-by assembly))))))

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

        assemblies (for [a (->> manifest :assemblies (filter select-assembly?))
                         :let [_ (infof "a is %s" a)]]
                     (merge a
                            {:fname (when (:components a)
                                      (str (clojure.core/name (:assembly a)) "-components"))
                             :components
                             (when (:components a)
                               (for [[n {component-ref :component
                                         using :using
                                         library-dependencies :library-dependencies
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

                                  {:library-dependencies
                                   (if component
                                     (concat
                                      (:library-dependencies (get components-by-id component-ref))
                                      library-dependencies)
                                     library-dependencies)}

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
                                   })))

                             }))

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
              :library-dependencies
              (->>
               (for [asmbly assemblies
                     c (:components asmbly)
                     dep (:library-dependencies c)]
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

              :dependencies
              (pr-str
               (->>
                (for [a assemblies
                      :let [mkey #(if (keyword? %) (make-key (:assembly a) %)
                                      (apply make-key %))]

                      [k v] (:dependencies a)
                      [n v] (ensure-map v)
                      ]
                  [(mkey k) [n (mkey v)]])
                ;; This call to 'first' should actually check to ensure
                ;; there aren't multiple entries, if there are it means
                ;; we have a conflict - more than one dependency is trying to bind
                (gbf #(into {} (gbf first %)))
                (into {})
                ))

              :files (mapcat :files assemblies)

              #_(->> assemblies
                     (mapcat :dependencies)
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
                    (mapcat :dependencies)
                    (group-by first)
                    (reduce-kv (fn [acc k v]
                                 (assoc acc k
                                        (into {}
                                              (mapcat
                                               (comp ensure-map second) v))))
                               {})))

    (main/info (format "Generating a new modular project named %s with options :-\n%s"
                       name
                       (->> manifest :assemblies
                            (filter select-assembly?)
                            (map :assembly)
                            (interpose "\n")
                            (apply str))))

    (letfn [(proc-file [{:keys [target template close-parens?]}]
              [target (cond-> (render template data) close-parens? close-parens)])]
      (apply ->files data (map proc-file (:files data))))

    #_(apply ->files data (process-files (:files data))
             ;; Core files
             ["project.clj" (render "project.clj" data)]
             ["src/{{sanitized}}/system.clj" (close-parens (render "system.clj" data))]
             ["dev/dev.clj" (render "dev.clj" data)]
             ["dev/user.clj" (render "user.clj" data)]

             ["dev/dev_components.clj" (render "dev_components.clj" data)]

             #_["src/{{sanitized}}/main.clj" (render "main.clj" data)]
             #_["src/{{sanitized}}/website.clj" (render "website.clj" data)]

             ;; Our Hello World! handler
             #_["src/{{sanitized}}/simple_webservice.clj" (render "simple_webservice.clj" data)]

             ;; TODO Write website.clj in terms of boilerplate.clj, it is currently too 'standalone'
             #_["src/{{sanitized}}/boilerplate.clj" (render "boilerplate.clj" data)]

             #_["src/{{sanitized}}/example_page.clj" (render "example_page.clj" data)]

             #_["src/{{sanitized}}/restricted_page.clj" (render "restricted_page.clj" data)]

             #_["test/{{sanitized}}/website_tests.clj" (render "website_tests.clj" data)]

             #_["src-cljs/{{sanitized}}/main.cljs" (render "main.cljs" data)]

             ;; Log configuration
             #_["resources/logback.xml" (render "logback.xml" data)]

             ;; HTML
             #_["resources/templates/page.html.mustache" (render "page.html.mustache")]
             #_["resources/templates/home.html.mustache" (render "home.html.mustache")]

             ;; CSS
             #_["resources/public/css/bootstrap.min.css" (render "resources/bootstrap.min.css")]
             #_["resources/public/css/bootstrap-theme.min.css" (render "resources/bootstrap-theme.min.css")]

             #_["resources/public/css/theme.css" (render "resources/theme.css")]

             ;; JS
             #_["resources/public/js/bootstrap.min.js" (render "resources/bootstrap.min.js")]
             #_["resources/public/js/jquery.min.js" (render "resources/jquery-2.1.1.min.js")]
             #_["resources/public/js/jquery.min.map" (render "resources/jquery-2.1.1.min.map")]
             #_["resources/public/js/react.js" (render "resources/react-0.9.0.js")]

             )))
