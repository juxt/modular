;; Copyright © 2015 JUXT LTD.

(ns modular.sendgrid.schema
  (:require
   [schema.core :as s]))

(def SendgridEmailer
  {:sendgrid
   {:api-user s/Str
    :api-password s/Str}})
