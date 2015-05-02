;; Copyright © 2015 JUXT LTD.

(ns modular.sendgrid
  (:require
   [modular.email.protocols :as p]
   [org.httpkit.client :refer (request)]
   [schema.core :as s]
   [modular.sendgrid.schema :as sc]
   [modular.email :as e]))

(defrecord SendgridEmailer [sendgrid]
  p/Emailer
  (send-email! [component email]
    (let [response
          @(request
            (merge
             {:method :post
              :url "https://api.sendgrid.com/api/mail.send.json"
              :headers
              {"Content-Type" "application/json"
               "Accept" "application/json"}}
             {:form-params
              {"api_user" (:user sendgrid)
               "api_key" (:password sendgrid)
               "to" (:to email)
               "from" (:from email)
               "text" (:body email)
               "subject" (:subject email)
               }})
            identity)]
      response)))

(defn new-sendgrid-mailer [& {:as opts}]
  (->> opts
       (merge {})
       (s/validate sc/SendgridEmailer)
       ->SendgridEmailer))
