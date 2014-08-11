(ns modular.menu.cylon
  (:require
   [modular.menu :refer (MenuItems)]
   [cylon.impl.authentication :as cylon]
   [schema.core :as s])
  (:import (cylon.impl.authentication LoginForm)))

;; Cylon

(defrecord LoginFormMenuItems
  MenuItems
  (menu-items [component] [{:label (:label component)
                            :target :cylon.impl.authentication/GET-login-form
                            :order \Z}]))

(def new-login-form-schema {:label s/Str})

(defn new-login-form-menu-items
  "Create a new login form component that has a specific label for its menu-item.
  menu-items."
  [& {:as opts}]
  (->> opts
       (merge {:label "Login"})
       (s/validate new-login-form-schema)
       map->LoginFormMenuItems))
