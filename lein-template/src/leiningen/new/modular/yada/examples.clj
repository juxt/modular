(ns {{name}}.examples
  (:require
   [clojure.java.io :as io]
   [com.stuartsierra.component :refer (Lifecycle using)]
   [modular.component.co-dependency :refer (co-using)]
   [modular.component.co-dependency.schema :as sc]
   [aleph.http :as http]
   [hiccup.core :refer (html)]
   [bidi.bidi :refer (tag path-for)]
   [bidi.ring :refer (make-handler redirect)]
   [schema.core :as s]
   [yada.yada :refer (yada)]
   yada.file-resource))

(defn hello
  "Say hello to an audience"
  [audience]
  (yada (str "Hello " audience "!")))

(defn source-code
  "Export a directory of source code to the web"
  []
  (yada (io/file "src")))

(defn index
  "An example of how to create an index page of all the other examples."
  []
  (yada
   (fn [ctx] ; we need the context, so we use a function which returns a string
     (let [routes (-> ctx :request :routes)]
       (assert routes "Routes should be injected into the Ring request")
       (html [:ul
              [:li [:a {:href (path-for routes ::hello)} "Hello"]]
              [:li [:a {:href (path-for routes ::src)} "Source code directory"]]])))
   :produces "text/html" ; the content-type for the string should be text/html
   ))

(defn make-routes []
  [""
   [["/" [["index.html" (tag (index) ::index)]
          ["" (redirect ::index)]
          ["hello" (-> (hello "World") (tag ::hello))]
          ["src/" (-> (source-code) (tag ::src))]]]
    ;; Provide a catch-all route
    [true (fn [req] {:status 404 :body "No example found"})]]])

(defn wrap-inject-routes
  "We use this middleware to inject the bidi route structure into the
  request so it may be used in calls to bidi's path-for
  function. Injecting data in the request like this is not a recommended
  approach but done here for pedagogical reasons. It is recommended that
  larger projects use the more general dependency injection approach
  described at https://modularity.org"
  [h routes]
  (fn [req]
    (h (assoc req :routes routes))))

(defn get-handler
  "Create the Ring handler for this server"
  []
  (let [routes (make-routes)]
    (-> (make-handler routes) ; bidi's make-handler
        (wrap-inject-routes routes))))

(s/defrecord Examples [port :- (s/named s/Int "Web server port")
                       server :- (s/protocol aleph.netty/AlephServer)]
  Lifecycle
  (start [component]
         ;; We use the start phase to start an Aleph server
         (let [server (http/start-server (get-handler) component)]
           (assoc component
                  :server server
                  :port (if (pos? port) port (aleph.netty/port server)))))
  (stop [component]
        (when-let [server (:server component)] (.close server))
        (dissoc component :server)))

(defn new-examples [& {:as opts}]
  (map->Examples opts))
