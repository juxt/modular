(ns {{name}}.example-page
  (:require
    [modular.menu :refer (MenuItems)]
    [modular.bidi :refer (WebService)])
)

(defrecord ExamplePage []
  MenuItems
  (menu-items [this] [{:label "Page" :target ::page}])
  WebService
  (request-handlers [_] {::page (fn [req] {:status 200 :body "PAGE"})})
  (routes [_] ["/" {"page" ::page}])
  (uri-context [_] "")
)

(defn new-example-page []
  (->ExamplePage)
  )
