;; Copyright © 2014 JUXT LTD.
(ns modular.logback-classic.core
  (:import [ch.qos.logback.classic Logger Level]
           [org.slf4j LoggerFactory]))

(defn ^Logger get-logger
  ([^String ns-logger]
     (LoggerFactory/getLogger ns-logger)))

(defn as-level [level]
  (cond
   (keyword? level) (get {:all Level/ALL
                          :debug Level/DEBUG
                          :error Level/ERROR
                          :info Level/INFO
                          :off Level/OFF
                          :trace Level/TRACE
                          :warn Level/WARN} level)
   (instance? Level level) level))

(defn add-appenders [ns-str level appender-seq]
  (let [lo (get-logger ns-str)]
    (.setLevel lo (as-level level))
    (doseq [appender appender-seq]
      (.addAppender lo appender))))
