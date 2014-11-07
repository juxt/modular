;; Copyright © 2014 JUXT LTD.

(ns modular.clostache
  (:require
   [modular.template :refer (Templater)]
   [clostache.parser :as parser]))

(defrecord ClostacheTemplater []
  Templater
  (render-template [_ template model]
    (parser/render-resource template model)))

(defn new-clostache-templater [& {:as opts}]
  (->> opts
       map->ClostacheTemplater))
