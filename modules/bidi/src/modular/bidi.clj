;; Copyright © 2014 JUXT LTD.

(ns modular.bidi
  (:require
   [schema.core :as s]
   [modular.ring :refer (WebRequestHandler)]
   [com.stuartsierra.component :as component :refer (Lifecycle)]
   [bidi.bidi :as bidi :refer (match-route resolve-handler RouteProvider tag)]
   [bidi.ring :refer (resources-maybe make-handler redirect archive)]
   [clojure.tools.logging :refer :all]
   [clojure.java.io :as io]))

(defrecord WebResources [uri-context resource-prefix key maybe?]
  RouteProvider
  (routes [_]
    [uri-context
     (-> ((if maybe?
            bidi.ring/resources-maybe
            bidi.ring/resources)
          {:prefix resource-prefix})
         (tag key))]))

(def new-web-resources-schema
  {:uri-context s/Str
   :resource-prefix s/Str
   :key s/Keyword
   :maybe? s/Bool})

(defn new-web-resources [& {:as opts}]
  (->> opts
       (merge {:uri-context "/"
               :resource-prefix ""
               :key :web-resources
               :maybe? true})
    (s/validate new-web-resources-schema)
    (map->WebResources)))

(defrecord ArchivedWebResources [archive uri-context resource-prefix]
  RouteProvider
  (routes [_] [uri-context (bidi.ring/archive
                            {:archive archive
                             :resource-prefix resource-prefix})]))

(def new-archived-web-resources-schema
  {:archive (s/protocol clojure.java.io/IOFactory)
   :uri-context s/Str
   :resource-prefix s/Str})

(defn new-archived-web-resources [& {:as opts}]
  (->> opts
       (merge {:uri-context "/"
               :resource-prefix "/"})
    (s/validate new-archived-web-resources-schema)
    (map->ArchivedWebResources)))

(defrecord Redirect [from to]
  RouteProvider
  (routes [_]
    [from (redirect to)]))

(defn new-redirect [& {:as opts}]
  (->> opts
    (merge {:from "/"})
    (s/validate {:from s/Str :to (s/either s/Keyword s/Str)})
    map->Redirect))

;; -----------------------------------------------------------------------

(defn as-request-handler
  "Convert a RouteProvider component into Ring handler."
  [service not-found-handler]
  (assert (satisfies? RouteProvider service))
  (some-fn
   (make-handler
    (cond
      (satisfies? RouteProvider service)
      (bidi/routes service)))

   not-found-handler))

(defrecord Router [not-found-handler]
  component/Lifecycle
  (start [component]
    (assoc component
           :routes ["" (vec
                        (remove nil?
                                (for [[ckey v] component]
                                  (when (satisfies? RouteProvider v)
                                    (bidi/routes v)))))]))
  (stop [this] this)

  RouteProvider
  (routes [this] (:routes this))

  WebRequestHandler
  (request-handler [this] (as-request-handler this not-found-handler)))

(def new-router-schema
  {:not-found-handler (s/=>* {:status s/Int
                              s/Keyword s/Any}
                             [{:uri s/Str
                               s/Keyword s/Any}])})

(defn new-router
  "Constructor for a ring handler that collates all bidi routes
  provided by its dependencies."
  [& {:as opts}]
  (->> opts
    (merge {:not-found-handler (constantly {:status 404 :body "Not found"})})
       (s/validate new-router-schema)
       map->Router))

(defn path-for
  "Convenience function wrapping bidi's path-for"
  [router target & args]
  #_(assert (satisfies? Router router) "Router argument must satisfy")
  (apply bidi/path-for (:routes router) target args))

;; ------  TODO Router needs to display all possible routes available,
;; ------  as debug data, so that people can see easily which routes are
;; ------  available. This addresses one of the more difficult and
;; ------  potentially frustrating cases of "computer says no" when the
;; ------  URI doesn't seem to dispatch to anything and no clues as to
;; ------  why! These routes can be determined by a tree walk.
