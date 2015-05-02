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
;; since they code the module keyword in a data-structure rather than
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
    :keyword (keyword (apply str (interpose "#" (map #(if % (clojure.core/name %) nil) [ns n]))))))

(defn gbf
  "Group by first and apply function to values"
  [f seq] (reduce-kv
           (fn [a k v] (conj a [k (f (map second v))]))
           []
           (group-by first seq)))

(defn load-manifest [f name]
  (load-edn-string
   (stencil/render-string
    (slurp f)
    {:name name
     :dev-password (format "\"%s\"" (generate-password))
     :sanitized (name-to-path name)})))

(defn usage []
  (println "Usage: lein new modular <project-name> <template> [ <arg>... ]"))

(defn list-templates [manifest]
  (println "Available templates")
  (println "-------------------")
  (let [tabstop (inc (apply max (map count (keys (:application-templates manifest)))))]
    (doseq [[^String k v] (sort-by first (:application-templates manifest))]
      (println (format "%s%s%s" k (apply str (repeat (max 1 (- tabstop (.length k))) \space)) (:description v) )))))

(defn clean [text]
  (apply str (interpose " " (clojure.string/split text #"\s+"))))

(defn word-wrap [s]
  (->> s
    clean
    (#(str % " "))           ; add a space (to ensure remainder matches)
    (re-seq #".{0,70}\s")   ; this regex
    (interpose (str \newline "   "))    ; format ecah line
    (apply str)                         ; join up
    ;; remove the space
    (#(subs % 0 (dec (count %))))))

(defn modular
  "Create a new modular project - TODO documentation show go here which
  will be shown on 'lein new :show modular' , but will only appear when
  released."

  ([name app-template & args]

   (let [augment-by (->> args
                         (keep (partial re-matches #"\+(.*)") )
                         (map second)   ; take the grouping
                         (map #(split % #"/"))
                         (map (partial apply keyword))
                         set)

         diminish-by (->> args
                          (keep (partial re-matches #"\-(.*)") )
                          (map second)  ; take the grouping
                          (map #(split % #"/"))
                          (map (partial apply keyword))
                          set)

         manifest (load-manifest (io/resource "manifest.edn") name)

         _ (when-not (get-in manifest [:application-templates app-template :modules])
             (throw (ex-info (format "No such template: %s" app-template)
                             {:template app-template})))

         select-module?
         (fn [{:keys [module]}]
           (when-let [includes
                      (conj (get-in manifest [:application-templates app-template :modules]) :core)]
             (or (contains? augment-by module)
                 (and (includes module)
                      (not (contains? diminish-by module))))))

         component-keys (->> manifest :modules
                             (filter select-module?)
                             (mapcat :components) vals set)

         components (->> manifest
                         :components
                         (filter (comp component-keys :component)))

         components-by-id (->> manifest
                               :components
                               (map (juxt :component identity))
                               (into {}))

         modules (for [a (->> manifest :modules (filter select-module?))]
                   (merge a
                          {:fname (when (:components a)
                                    (str (clojure.core/name (:module a)) "-components"))

                           :docstring (if (:docstring a)
                                        (str \newline "  \"" (word-wrap (:docstring a)) "\"") "")

                           :components
                           (when (:components a)
                             (for [[n {component-ref :component
                                       using :using
                                       co-using :co-using
                                       library-dependencies :library-dependencies
                                       args :args
                                       :as instance}]
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

                                {:key (make-key (:module a) n)
                                 :refers (conj (:refers instance) constructor)
                                 :constructor (symbol (clojure.core/name constructor))
                                 :args (map #(cond (string? %) % #_(str "\"" % "\"") :else %) args)

                                 ;; Construct 'using' here, not in template
                                 ;; it could be a map, it could be a vector

                                 :using (pr-str
                                         (or (:using instance) []))

                                 :co-using (pr-str
                                            (or (:co-using instance) []))

                                 :pad10 (apply str (repeat (+ 10 (count (str n))) \space))
                                 :pad18 (apply str (repeat (+ 18 (count (str n))) \space))
                                 })))}))

         settings (let [f (io/file (System/getProperty "user.home") ".lein/modular.edn")]
                    (when (.exists f)
                      (read
                       (indexing-push-back-reader
                        (java.io.PushbackReader. (io/reader f))))))

         data {:name name
               :user (or (-> settings :github :user) (System/getenv "USER"))
               :year (str (.get (java.util.Calendar/getInstance) java.util.Calendar/YEAR))
               :sanitized (name-to-path name)
               :snake-cased-name (clojure.string/replace name #"_" "-")

               :modules modules

               :module? (zipmap (set (map :module modules)) (repeat true))

               :template app-template

               ;; Probably need to be sorted and with some conflict
               ;; resolution warnings. Design decision is to generate
               ;; anyway (the developer can always delete and start
               ;; over). Also, developers can be given a warning (ala
               ;; pacman on Arch) to tell them to expect conflicts -
               ;; these can be resolved later
               :library-dependencies
               (->>
                (for [module modules
                      ;; this conj ensures module-level library dependencies are included
                      c (conj (:components module) module)
                      dep (:library-dependencies c)]
                  dep)
                sort distinct)

               :refers
               (->>
                (for [module modules
                      c (:components module)
                      refer (:refers c)]
                  refer)
                sort distinct
                (group-by (comp symbol namespace))
                (reduce-kv
                 (fn [a k v] (conj a {:namespace k
                                      :refers (apply str (interpose " " (distinct (map (comp clojure.core/name) v))))}))
                 []))

               #_:dev-refers
               #_(->>
                (for [module modules
                      refer (:dev-refers module)]
                  refer)
                sort distinct
                (group-by (comp symbol namespace))
                (reduce-kv
                 (fn [a k v] (conj a {:namespace k
                                      :refers (apply str (interpose " " (distinct (map (comp clojure.core/name) v))))}))
                 []))

               :dev-snippets
               (apply str
                      (interpose "\n\n"
                                 (for [snippet (mapcat :dev-snippets modules)]
                                   (slurp (render (:template snippet))))))

               ;; Dependency maps are be useful for adding dependencies
               ;; to components that already exist, such as template
               ;; models and menus

               :dependencies
               (->>
                (for [m modules
                      :let [mkey #(if (keyword? %) (make-key (:module m) %)
                                      (apply make-key %))]

                      [k v] (:dependencies m)
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
                (for [m modules
                      :let [mkey #(if (keyword? %) (make-key (:module m) %)
                                      (apply make-key %))]

                      [k v] (:co-dependencies m)
                      [n v] (ensure-map v)
                      ]
                  [(mkey k) [n (mkey v)]])
                ;; This call to 'first' should actually check to ensure
                ;; there aren't multiple entries, if there are it means
                ;; we have a conflict - more than one dependency is trying to bind
                (gbf #(into {} (gbf first %)))
                (into {}))

               :files (concat (get-in manifest [:application-templates app-template :files] [])
                              (mapcat :files modules))}]

     (main/info (format "Generating a new modular project named %s with modules :-\n%s"
                        name
                        (->> manifest :modules
                             (filter select-module?)
                             (map (comp clojure.core/name :module))
                             (interpose ", ")
                             (apply str))))

     (letfn [(proc-file [{:keys [target template close-parens? file]}]
               (cond
                 template
                 [target (cond-> (render template (merge settings data)) close-parens? close-parens)]

                 file
                 [target (if-let [res
                                  (io/resource (clojure.string/join "/" ["leiningen" "new" "modular" file]))]
                           (io/input-stream res)
                           (throw (ex-info (format "Cannot load resource: '%s'" file) {:file file}))

                           )]))]

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
  ([name]
   (usage)
   (println)
   (list-templates (load-manifest (io/resource "manifest.edn") name))))
