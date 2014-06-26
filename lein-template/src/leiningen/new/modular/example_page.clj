(ns {{name}}.example-page
  (:require
   [clojure.string :as str]
   [modular.menu :refer (MenuItems)]
   [modular.bidi :refer (WebService)]))

(defrecord ExamplePage [label content key path]
  MenuItems
  (menu-items [this] [{:label label :target key}])
  WebService
  (request-handlers [_]
    {key (fn [req] {:status 200
                    :headers {"Content-Type" "text/html"}
                    :body (str "<h2>" content "</h2>")})})
  (routes [_] ["" {path key}])
  (uri-context [_] ""))

(defn new-example-page [& {:as opts}]
  (let [label (or (:label opts) "Page")
        sanitized (str/replace label #"\s+" "")]
    (->> opts
         (merge {:label label
                 :text "Blank"
                 :key (keyword (.toLowerCase sanitized))
                 :path (str "/" sanitized)})
         map->ExamplePage)))
