;; Copyright © 2014 JUXT LTD.
(ns modular.logback-classic.logger
    (:require [com.stuartsierra.component :as component :refer (Lifecycle)]
              [schema.core :as s]
              [modular.logback-classic.core :refer :all]
              [modular.logback-classic.appenders :refer (console-appender file-appender)])
    (:import [ch.qos.logback.core Appender]))


(defrecord LoggerConf [appenders root-level loggers]
  Lifecycle
  (start [component]
    (add-appenders "root" root-level appenders)
    (doseq [[logger-ns logger-level] loggers]
      (.setLevel (get-logger logger-ns) (as-level logger-level)))
    component)
  (stop [component]
    (.detachAndStopAllAppenders (get-logger "root"))
    (doseq [[logger-ns _] loggers]
      (.detachAndStopAllAppenders (get-logger logger-ns)))
    component))

(def s-levels (s/enum :all :debug :error :info :off :trace :warn))

(defn new-logger-conf [file-name & {:as opts}]
  (->> opts
       (merge {:root-level :debug
               :appenders [(console-appender {:appender {:name "CONSOLE"}
                                              :encoder {:charset "UTF-8" :pattern "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"}
                                              :filter {:level :debug}})
                           (file-appender {:appender {:name "FILE" :file-name file-name}
                                           :rolling-policy {:file-name-pattern (str file-name ".%d{yyyy-MM-dd}.%i.log") :max-history "30"}
                                           :triggering-policy {:max-file-size "50MB"}
                                           :encoder {:charset "UTF-8" :pattern "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"}})]})
       (s/validate {:root-level s-levels
                    :appenders [Appender]
                    (s/optional-key :loggers) {s/Str s-levels}})
       map->LoggerConf))
