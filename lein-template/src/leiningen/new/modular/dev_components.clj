(ns dev-components
  (:require
   [com.stuartsierra.component :refer (Lifecycle using)]
   [cylon.user :refer (add-user!)]
   [schema.core :as s]
   [modular.ring :refer (WebRequestMiddleware)]
   [plumbing.core :refer (<-)]))

(defrecord UserDomainSeeder [users]
  Lifecycle
  (start [component]
    (doseq [{:keys [id password]} users]
      (do
        (println (format "Adding user '%s' with password: %s" id password))
        (add-user! (:cylon/user-domain component) id password {:name "Development user"})))
    component)
  (stop [component] component))

(defn new-user-domain-seeder [& {:as opts}]
  (->> opts
       (s/validate {:users [{:id s/Str :password s/Str}]})
       map->UserDomainSeeder
       (<- (using [:cylon/user-domain]))))

(defn wrap-schema-validation [h]
  (fn [req]
    (s/with-fn-validation
      (h req))))
