;; Copyright © 2014 JUXT LTD.
(ns modular.logback-classic.appenders
  (:require [modular.logback-classic.core :refer (get-logger as-level)])
  (:import
   [java.nio.charset Charset]
   [org.slf4j LoggerFactory]
   [ch.qos.logback.core ConsoleAppender]
   [ch.qos.logback.core.rolling RollingFileAppender SizeAndTimeBasedFNATP TimeBasedRollingPolicy]
   [ch.qos.logback.classic.encoder PatternLayoutEncoder]
   [ch.qos.logback.classic.filter ThresholdFilter]))

(defn console-appender [{{name :name} :appender
                         {:keys [charset pattern]} :encoder
                         {level :level} :filter}]
  (let [context (LoggerFactory/getILoggerFactory)
        filter (ThresholdFilter.)
        encoder (PatternLayoutEncoder.)
        rf-appender (ConsoleAppender.)]

    (doto rf-appender
      (.setName name)
      (.setContext context))

    (doto encoder
      (.setContext context)
      (.setPattern pattern)
      (.setCharset (Charset/forName charset))
      (.start))

    (doto filter
      (.setLevel (str (as-level level)))
      (.setContext context)
      (.start))

    (doto rf-appender
      (.setEncoder encoder)
      (.addFilter filter)
      (.start))))

(defn file-appender [{{:keys [name file-name]} :appender
                      {:keys [file-name-pattern max-history]} :rolling-policy
                      {max-file-size :max-file-size} :triggering-policy
                      {:keys [charset pattern]} :encoder}]
  (let [context (LoggerFactory/getILoggerFactory)
        triggering-policy (SizeAndTimeBasedFNATP.)
        rolling-policy (TimeBasedRollingPolicy.)
        encoder (PatternLayoutEncoder.)
        rf-appender (RollingFileAppender.)]

    (doto rf-appender
      (.setName name)
      (.setContext context)
      (.setFile file-name))


    (doto rolling-policy
      (.setContext context)
      (.setParent rf-appender)
      (.setFileNamePattern file-name-pattern)
      (.setTimeBasedFileNamingAndTriggeringPolicy triggering-policy)
      (.start))

    (doto triggering-policy
      (.setContext context)
      (.setMaxFileSize max-file-size)
      (.setTimeBasedRollingPolicy rolling-policy)
      (.start))

    (doto encoder
      (.setContext context)
      (.setPattern pattern)
      (.setCharset (Charset/forName charset))
      (.start))

    (doto rf-appender
      (.setFile file-name)
      (.setEncoder encoder)
      (.setRollingPolicy rolling-policy)
      (.setTriggeringPolicy triggering-policy)
      (.start))))
