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
  MenuItems
  (menu-items [this context]
    (->> this
         vals
         (filter (partial satisfies? MenuItems))
         (mapcat #(menu-items % context))
         (remove nil?)
         (sort-by :order)
         (group-by :parent)
         seq
         (sort-by (comp nil? first)))))

(defn new-menu-index []
  (->MenuIndex))

(defrecord BootstrapMenu []
  TemplateModel
  (template-model [this {{routes :modular.bidi/routes :as req} :request :as context}]
    (let [menu (menu-items (:menu-index this) context)]
      (println "menu is " menu)
      {:menu
       (html
        (apply concat
               (for [[parent items] menu]
                 (let [listitems
                       (for [{:keys [href order label args]} items]
                         [:li (if href
                                [:a {:href (apply path-for routes href args)} label]
                                [:a {:href "#"} label])])]
                   (if parent
                     (list
                      [:li.dropdown
                       [:a.dropdown-toggle {:href "#" :data-toggle "dropdown"} parent [:b.caret]]
                       [:ul.dropdown-menu listitems]])
                     listitems)))))})))

(defn new-bootstrap-menu []
  (component/using (->BootstrapMenu) [:menu-index]))
