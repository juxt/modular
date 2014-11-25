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

        assemblies (for [a (->> manifest :assemblies (filter select-assembly?))]
                     (merge a
                            {:fname (when (:components a)
                                      (str (clojure.core/name (:assembly a)) "-components"))

                             :components
                             (when (:components a)
                               (for [[n {component-ref :component
                                         using :using
                                         co-using :co-using
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
                                   :args args

                                   ;; Construct 'using' here, not in template
                                   ;; it could be a map, it could be a vector

                                   :using (pr-str
                                           (or (:using instance) []))

                                   :co-using (pr-str
                                              (or (:co-using instance) []))

                                   :pad10 (apply str (repeat (+ 10 (count (str n))) \space))
                                   :pad18 (apply str (repeat (+ 18 (count (str n))) \space))
                                   })))

                             }))

        settings (when-let [f (io/file (System/getProperty "user.home")
                                       ".lein/modular.edn")]
                   (read
                    (indexing-push-back-reader
                     (java.io.PushbackReader. (io/reader f)))))

        data {:name name
              :year (str (.get (java.util.Calendar/getInstance) java.util.Calendar/YEAR))
              :sanitized (name-to-path name)
              :snake-cased-name (clojure.string/replace name #"_" "-")

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
                     ;; this conj ensures assembly-level library dependencies are included
                     c (conj (:components asmbly) asmbly)
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
                []))

              ;; Dependency maps are be useful for adding dependencies
              ;; to components that already exist, such as template
              ;; models and menus

              :dependencies
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
               )

              :co-dependencies
              (->>
               (for [a assemblies
                     :let [mkey #(if (keyword? %) (make-key (:assembly a) %)
                                     (apply make-key %))]

                     [k v] (:co-dependencies a)
                     [n v] (ensure-map v)
                     ]
                 [(mkey k) [n (mkey v)]])
               ;; This call to 'first' should actually check to ensure
               ;; there aren't multiple entries, if there are it means
               ;; we have a conflict - more than one dependency is trying to bind
               (gbf #(into {} (gbf first %)))
               (into {})
               )

              :files (mapcat :files assemblies)

              }]

    (main/info (format "Generating a new modular project named %s with modules :-\n%s"
                       name
                       (->> manifest :assemblies
                            (filter select-assembly?)
                            (map (comp clojure.core/name :assembly))
                            (interpose ", ")
                            (apply str))))

    (letfn [(proc-file [{:keys [target template close-parens? file]}]
              (cond
               template
               [target (cond-> (render template (merge settings data)) close-parens? close-parens)]

               file
               [target (render file)]))]
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


             )))
