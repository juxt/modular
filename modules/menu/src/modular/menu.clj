;; Copyright © 2014 JUXT LTD.

(ns modular.menu
  (:require
   [com.stuartsierra.component :as component]
   [modular.web-template :refer (DynamicTemplateData)]
   [bidi.bidi :refer (path-for)]
   [hiccup.core :refer (html)]
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

(defn new-menu-index []
  (->MenuIndex))

(defrecord BootstrapMenu []
  DynamicTemplateData
  (dynamic-template-data [this {routes :modular.bidi/routes :as req}]
    (assert routes
            "routes cannot be nil, menus must be fronted with a bidi router component so that uris can be generated")
    (let [menu (menu-items (:menu-index this))]
      {:menu
       (html
        (apply concat
               (for [[parent items] menu]
                 (let [listitems
                       (remove nil?
                               (for [{:keys [target order label args visible?] :or {visible? (constantly true)} :as ctx} items]
                                 (when (visible? (-> ctx
                                                     (assoc :request req)
                                                     (dissoc :visible?)))

                                   ;;                                   (println "target:" target)
                                   ;;                                   (println "routes:" routes)
                                   ;;                                   (println "uri:" (apply path-for routes target args))


                                   [:li (when (= label "Starbuck") {:class "active"})
                                    (if target
                                      [:a {:href (apply path-for routes target args)} label]
                                      ;; To render properly in bootstrap, need this to be an a element.
                                      [:a {:href "#"} label])])))]
                   (if parent
                     (list
                      [:li.dropdown
                       [:a.dropdown-toggle {:data-toggle "dropdown"} parent [:b.caret]]
                       [:ul.dropdown-menu listitems]])
                     listitems)))))})))

(defn new-bootstrap-menu []
  (component/using (->BootstrapMenu) [:menu-index]))


(defrecord SideMenu []
  DynamicTemplateData
  (dynamic-template-data [this {routes :modular.bidi/routes :as req}]
    (let [menu (menu-items (:menu-index this))]
      {:menu
       (html
        (apply concat
               (for [[parent items] menu]
                 (let [listitems
                       (remove nil?
                               (for [{:keys [target order label args visible?] :or {visible? (constantly true)} :as ctx} items]
                                 (when (visible? (-> ctx
                                                     (assoc :request req)
                                                     (dissoc :visible?)))
                                   [:li [:a {:href (apply path-for routes target args)} label]])))]

                   (if (and parent (not-empty listitems))
                     (list
                      [:li [:a {:data-toggle "collapse"
                                :data-parent "#accordion"
                                :href (str "#" parent)
                                :class "collapsed"
                                } parent]]
                      [:div {:id (str parent) :class "collapse out"}
                       [:ul listitems]])
                     listitems
                     )))))})))

(defn new-side-menu []
  (component/using (->SideMenu) [:menu-index]))
