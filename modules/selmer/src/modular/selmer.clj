;; Copyright © 2015 JUXT LTD.

(ns modular.selmer
  (:require [modular.template :refer [Templater]]
            [selmer.parser :refer [render]]))

(defrecord SelmerTemplater []
  Templater
  (render-template [_ template model]
    (render template model)))

(defn new-selmer-templater [& {:as opts}]
  (->> opts
       map->SelmerTemplater))
