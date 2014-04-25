;; Copyright © 2014 JUXT LTD.

(ns modular.menu
  (:require
   [com.stuartsierra.component :as component]
   [modular.template :refer (TemplateModel)]
   [bidi.bidi :refer (path-for)]
   [hiccup.core :refer (html)]))

(defprotocol MenuItems
  (menu-items [_ context]))

(defrecord MenuIndex []
  TemplateModel
  (template-model [this context]
    (let [req (:request context)
          routes (-> req :modular.bidi/routes)]
      {:menu (for [{:keys [label href]}
                   (->> this vals
                        (filter (partial satisfies? MenuItems))
                        (mapcat #(menu-items % context))
                        (sort-by :order))]
               {:listitem (html [:li [:a {:href (path-for routes href)} label]])}) })))

;; [:a {:href (path-for routes handler)} label]

(defn new-menu-index []
  (->MenuIndex))
