(ns {{name}}.generate
  (:require
   [modular.ring :refer (request-handler)]
   [clojure.pprint :refer (pprint)]
   [clojure.java.io :as io]
   [clojure.set :as set])
  (:gen-class))

(defprotocol Body
  (serialize [_]))

(extend-protocol Body
  String
  (serialize [s] s)
  java.io.File
  (serialize [f] (slurp f))
  sun.net.www.protocol.jar.JarURLConnection$JarURLInputStream
  (serialize [_] ""))

(defn get-links [type body]
  (case type
    "text/html"
    (concat
     (->> (map second (re-seq #"href=\"([^\"]+)\"" body))
       (remove (some-fn #(re-matches #"https?://.*" %) #{"#"})))
     (->> (map second (re-seq #"<script.+src=\"([^\"]+)\"" body))
       (remove #(re-matches #"https?://.*" %)))
     (map second (re-seq #"url\('([^\']+)'\)" body))
     )
    "text/css"
    []))

(defn get-type [uri]
  (cond
    (re-matches #".*\.html" uri) "text/html"
    (re-matches #".*\.css" uri) "text/css"
    :otherwise (throw (ex-info "No type" {:uri uri}))))

(defn- status? [n response]
  (re-matches (re-pattern (str n "\\d\\d")) (str (:status response))))

(defn ok? [response]
  (status? 2 response))

(defn redirect? [response]
  (status? 3 response))

(defn client-error? [response]
  (status? 4 response))

(defn get-suffix [s]
  (second (re-matches #".*(\..*)" s)))

(def ^:dynamic *handler* nil)

(defn spider [visited request]
  (cond
    (#{".jpg" ".png" ".js"} (get-suffix (:uri request)))
    [(:uri request)]

    :otherwise
    (try
      (let [visited (conj visited (:uri request))
            response (*handler* request)]
        (cond
          (redirect? response)
          (conj (spider visited {:uri (get-in response [:headers "Location"])})
                [:redirect (:uri request)])

          (ok? response)
          (conj
           (distinct
            (apply concat
                   (for [link (set/difference
                               (set (get-links (get-type (:uri request))
                                               (serialize (:body response))))
                               visited)]
                     (spider visited {:uri link}))))
           (:uri request))

          (client-error? response)
          [[:not-found (:uri request)]]

          :otherwise [{:unexpected response
                       :request request}]
          ))
      (catch Exception e [{:error-with (:uri request)
                           :exception e
                           :stack-trace (seq (.getStackTrace e))}]))))

(defn -main [& args]
  (eval '(do (require '{{name}}.system)
             (require '{{name}}.generate)
             (require 'modular.ring)
             (require 'com.stuartsierra.component)
             {{#module?.co-dependency}}
             (require 'tangrammer.component.co-dependency)
             {{/module?.co-dependency}}

             (require 'clojure.java.browse)

             (println "Starting {{name}}")

             (let [system (->
                           ({{name}}.system/new-production-system)
                           {{^module?.co-dependency}}
                           com.stuartsierra.component/start
                           {{/module?.co-dependency}}
                           {{#module?.co-dependency}}
                           tangrammer.component.co-dependency/start-system
                           {{/module?.co-dependency}}
                           )]

               (println "System started")
               (println "Spidering...")

               (binding [{{name}}.generate/*handler* (-> system :modular-bidi-router-webrouter modular.ring/request-handler)]
                 (doseq [path (mapcat (partial {{name}}.generate/spider #{}) [{:uri "/"}])]
                   (println "> " path)))

               (println "Stopping {{name}}")
               (com.stuartsierra.component/stop system)
               (println "System stopped")

               ))))
