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
  (serialize [i] (slurp i)))

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
    [{:type :copy
      :uri (:uri request)}]

    :otherwise
    (try
      (let [visited (conj visited (:uri request))
            response (*handler* request)]
        (cond
          (redirect? response)
          (conj (spider visited {:uri (get-in response [:headers "Location"])})
                {:type :redirect
                 :uri (:uri request)})

          (ok? response)
          (let [sbody (serialize (:body response))]
            (conj
             (distinct
              (apply concat
                     (for [link (set/difference
                                 (set (get-links
                                       (get-type (:uri request))
                                       sbody))
                                 visited)]
                       (spider visited {:uri link}))))
             {:type :copy
              :uri (:uri request)}))

          (client-error? response)
          [{:type :not-found :uri (:uri request)}]

          :otherwise [{:type :unexpected
                       :respose response
                       :request request}]
          ))
      (catch Exception e [{:type :error
                           :uri (:uri request)
                           :exception e
                           :stack-trace (seq (.getStackTrace e))}]))))

(defmulti action (fn [x destdir] (:type x)))

(defmethod action :redirect [a destdir]
  (println "redirect - noop"))

(defmethod action :copy [{:keys [uri]} destdir]
  (try
    (let [response (*handler* {:uri uri})]
      (assert (= (:status response)200))
      (let [outfile (io/file (str destdir uri))
            data (.getBytes (serialize (:body response)))]
        (.mkdirs (.getParentFile outfile))
        (io/copy data outfile)
        (println (format "Copied %d bytes to %s" (count data) outfile))))
    (catch Exception e
      (println "Exception" e " on uri" uri)
      )))


(defn do-actions [actions]
  (let [destdir (io/file "target/site")]
    (.mkdirs destdir)
    (doseq [a actions]
      (action a destdir))))

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
                           ({{name}}.system/new-production-system
                            #_{:http-server {:port 3001}})
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
                 ({{name}}.generate/do-actions
                  (mapcat (partial {{name}}.generate/spider #{}) [{:uri "/"}])))

               (println "Stopping {{name}}")
               (com.stuartsierra.component/stop system)
               (println "System stopped")

               ))))
