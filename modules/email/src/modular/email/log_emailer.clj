;; Copyright © 2015 JUXT LTD.

(ns modular.email.log-emailer
  (:require
   [clojure.tools.logging :refer :all]
   [modular.email.protocols :as p]))

(defrecord LogEmailer []
  p/Emailer
  (send-email! [component email]
    (infof "Sending email: %s" email)))

(defn new-log-emailer [& {:as opts}]
  (->> opts
       (merge {})
       map->LogEmailer))
