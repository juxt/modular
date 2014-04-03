(ns {{snake-cased-name}}.system
  (:require [com.stuartsierra.component :as component]))

(defn new-system []
  (component/system-map 
   :a-component "Add your components here"))
