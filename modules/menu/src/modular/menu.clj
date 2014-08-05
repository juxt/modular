;; Copyright © 2014 JUXT LTD.

(ns modular.menu
  (:require
   [schema.core :as s]))

(defprotocol MenuItems
  (menu-items [_]))

(defrecord MenuIndex []
  MenuItems
  (menu-items [this]
    (->> this
         vals
         (filter (partial satisfies? MenuItems))
         (mapcat menu-items)
         (remove nil?)
         (sort-by :order)
         (group-by :parent)
         seq
         (sort-by (comp nil? first)))))

(defn new-menu-index [& {:as opts}]
  (->> opts
       (merge {:comparator :order})
       (s/validate {(s/optional-key :comparator) (s/pred ifn?)})
       map->MenuIndex))

(s/defn menu-items+ :- [{:label s/Str
                         :target s/Keyword
                         (s/optional-key :order) s/Any}]
  "A Schema checking version of menu-items."
  [component]
  (s/with-fn-validation
    (menu-items component)))
