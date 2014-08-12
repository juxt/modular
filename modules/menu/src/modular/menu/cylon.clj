(ns modular.menu.cylon
  (:require
   [modular.menu :refer (MenuItems)]
   [cylon.impl.authentication :as cylon]
   [schema.core :as s]))

;; Cylon

(defrecord LoginFormMenuItems [label]
  MenuItems
  (menu-items [component]
    [{:label label
      :target :cylon.impl.authentication/GET-login-form
      :order \Z}]))

(defn new-login-form-menu-items
  "Create a new login form component that has a specific label for its menu-item.
  menu-items."
  [& {:as opts}]
  (->> opts
       (merge {:label "Login"})
       (s/validate {:label s/Str})
       map->LoginFormMenuItems))
