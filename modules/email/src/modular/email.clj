;; Copyright © 2015 JUXT LTD.

(ns modular.email
  (:require
   [schema.core :as s]
   [modular.email.protocols :as p]))

(def MimeType s/Str)

(def FileAttachment
  {:type (s/enum :inline)
   :content-type MimeType
   (s/optional-key :content-id) s/Str
   (s/optional-key :file-name) s/Str
   (s/optional-key :description) s/Str})

(def BodyPart
  {:content s/Any
   :type MimeType})

(def Multipart
  [(s/one MimeType "mime-type")
   (s/either FileAttachment BodyPart
             (s/recursive #'Multipart))])

(s/defschema EmailAddress
  "An email address (relaxed version)"
  (s/pred (fn [s] (re-matches #"\S+@\S+" s))))

(s/defschema EmailMessage
  ""
  {:to EmailAddress
   :from EmailAddress
   :subject s/Str
   :body (s/either s/Str Multipart)
   ;; Optional headers allowed
   s/Keyword s/Str})

(s/defn send-email! :- nil
  [component :- (s/protocol p/Emailer)
   email :- EmailMessage]
  (p/send-email! component email))
