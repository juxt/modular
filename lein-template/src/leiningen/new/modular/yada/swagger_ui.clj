(ns {{name}}.swagger-ui
  (:require
   [clojure.java.io :as io]
   [com.stuartsierra.component :refer (using)]
   [modular.component.co-dependency :refer (co-using)]
   [modular.component.co-dependency.schema :refer (co-dep)]
   [hiccup.core :refer (html)]
   [bidi.bidi :refer (tag path-for RouteProvider)]
   [schema.core :as s]
   [yada.yada :refer (yada)]
   yada.file-resource)
  (:import (modular.bidi Router)))

(defn get-handler []
  (yada "swagger-ui"))

(s/defrecord SwaggerUi [*router :- (co-dep Router)]
  RouteProvider
  (routes [_] ["/swagger-ui" (get-handler)]))

(defn new-swagger-ui [& {:as opts}]
  (->
   (map->SwaggerUi opts)
   (co-using [:router])))
