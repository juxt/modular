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
  (menu-items [_ context]))

;; If there is a protection system... see accounting2 eae2f02 src/juxt/accounting/menu.clj
#_(when-let [{:keys [handlers]} (-> this :protection-system :login-form)]
                  [(if-not (:cylon.core/session req)
                     {:label "Login" :order "Z" :handler (:login handlers)}
                     {:label "Logout" :order "Z" :handler (:logout handlers)})])

(defrecord MenuIndex []
  MenuItems
  (menu-items [this context]
    (s/validate {:request {s/Keyword s/Any}} context)
    (->> this
         vals
         (filter (partial satisfies? MenuItems))
         (mapcat #(menu-items % context))
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
    (let [menu (menu-items (:menu-index this) context)]
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
                       [:a.dropdown-toggle {:data-toggle "dropdown"} parent [:b.caret]]
                       [:ul.dropdown-menu listitems]])
                     listitems)))))})))

(defn new-bootstrap-menu []
  (component/using (->BootstrapMenu) [:menu-index]))
