;; Copyright © 2014 JUXT LTD.

(ns modular.menu
  (:require
   [com.stuartsierra.component :as component]
   [modular.template :refer (TemplateModel)]
   [bidi.bidi :refer (path-for)]
   [hiccup.core :refer (html)]
   ;;[cylon.core :refer (allowed-handler?)]
   [schema.core :as s]))

#_(defn show-menu-item?
  "This is an example of how useful it is to wrap handlers in records
  that implement the AuthorizedHandler protocol. In this case, we can
  call authorized-handler? on any given handler to see whether it would
  be authorized with this request. We use this result to avoid
  displaying menu-items that are not available. We could alternatively
  use this information to disable or grey-out the menu-item."
  [req {:keys [handler label]}]
  (allowed-handler? handler req))

(defprotocol MenuItems
  (menu-items [_]))

;; If there is a protection system... see accounting2 eae2f02 src/juxt/accounting/menu.clj
#_(when-let [{:keys [handlers]} (-> this :protection-system :login-form)]
                  [(if-not (:cylon.core/session req)
                     {:label "Login" :order "Z" :handler (:login handlers)}
                     {:label "Logout" :order "Z" :handler (:logout handlers)})])

(defrecord MenuIndex []
  MenuItems
  (menu-items [this]
    (->> this
         vals
         (filter (partial satisfies? MenuItems))
         (mapcat menu-items)
         (remove nil?)
;;         (filter (partial show-menu-item? (:request context)))
         (sort-by :order)
         (group-by :parent)
         seq
         (sort-by (comp nil? first)))))

(defn new-menu-index []
  (->MenuIndex))

(defrecord BootstrapMenu []
  TemplateModel
  (template-model [this {{routes :modular.bidi/routes :as req} :request :as context}]
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
                                   [:li (if target
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
  TemplateModel
  (template-model [this {{routes :modular.bidi/routes :as req} :request}]
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
