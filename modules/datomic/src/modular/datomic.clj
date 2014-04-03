;; Copyright © 2014, JUXT LTD. All Rights Reserved.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns modular.datomic
  (:require
   [clojure.java.io :as io]
   [com.stuartsierra.component :as component]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
   [datomic.api :as d]
   [schema.core :as s]))

(defrecord EphemeralDatabase [uri]
  component/Lifecycle
  (start [this]
    (assoc this :database (d/create-database uri)))

  (stop [this]
    (d/delete-database uri)
    (d/shutdown false)
    this))

(defrecord DurableDatabase [uri]
  component/Lifecycle
  (start [this] this)
  (stop [this]
    (d/shutdown false)
    this))

(def new-datomic-database-schema
  {:uri s/Str
   :ephemeral? s/Bool})

(defn new-datomic-database [& {:as opts}]
  (let [{:keys [uri ephemeral?]} (->> opts
                           (merge {:ephemeral? false})
                           (s/validate new-datomic-database-schema))]
    (if ephemeral?
      (->EphemeralDatabase uri)
      (->DurableDatabase uri))))

(defrecord DatomicConnection []
  component/Lifecycle
  (start [this] (d/connect (get-in this [:database :uri])))
  (stop [this] this))

(defn new-datomic-connection []
  (component/using
   (->DatomicConnection)
   [:database]))

(defrecord DatomicSchema [res]
  component/Lifecycle
  (start [this]
    (with-open [rdr (java.io.PushbackReader. (io/reader res))]
      @(d/transact (:connection this)
                   (binding [clojure.tools.reader/*data-readers*
                             {'db/id datomic.db/id-literal
                              'db/fn datomic.function/construct
                              'base64 datomic.codec/base-64-literal}]
                     (clojure.tools.reader/read (indexing-push-back-reader rdr)))))
    this)
  (stop [this] this))

(defn new-datomic-schema [res]
  (component/using
   (->DatomicSchema res)
   [:connection]))


(defn create-functions [functions]
  (vec
   (for [[ident {:keys [doc params path]}] functions]
     {:db/id (d/tempid :db.part/user)
      :db/ident ident
      :db/doc doc
      :db/fn (d/function {:lang "clojure" :params params :code (slurp (io/resource path))})})))

(defrecord DatomicFunctions [functions]
  component/Lifecycle
  (start [this]
    @(d/transact (:connection this) (create-functions functions))
    this)
  (stop [this] this))

(defn new-datomic-functions [functions]
  (component/using
   (->DatomicFunctions functions)
   [:connection]))
