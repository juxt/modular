;; Copyright © 2015 JUXT LTD.

(ns modular.email.protocols)

(defprotocol Emailer
  (send-email! [_ data]
    "Send an email, the receipient, subject, body and other details
    being given as data."))
