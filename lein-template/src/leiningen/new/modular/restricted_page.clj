(ns {{name}}.restricted-page
  (:require
   [com.stuartsierra.component :as component]
   [clojure.string :as str]
   [modular.menu :refer (MenuItems)]
   [modular.bidi :refer (WebService)]
   [cylon.authorization :refer (restrict-handler)]))

(defrecord RestrictedPage []
  MenuItems
  (menu-items [this] [{:label "Secure Page" :target ::page}])

  WebService
  (request-handlers [this]
    {::page (-> (fn [req] {:status 200
                           :headers {"Content-Type" "text/html"}
                           :body (str "<h2>" "Bruce's Salary: $xxx,xxx" "</h2>")})
                (restrict-handler (:authorizer this) #{:admin}))})
  (routes [_] ["/restricted-page" ::page])
  (uri-context [_] ""))

(defn new-restricted-page [& {:as opts}]
  (component/using
   (->> opts
        map->RestrictedPage)
   [:authorizer]))
