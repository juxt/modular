;; Copyright © 2015 JUXT LTD.

(ns modular.stencil
  (:require
   [modular.template :refer (Templater)]
   [stencil.core :as stencil]))

(defrecord StencilTemplater []
  Templater
  (render-template [_ template model]
    (stencil/render-file template model)))

(defn new-stencil-templater [& {:as opts}]
  (->> opts
       map->StencilTemplater))
