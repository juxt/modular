(ns {{name}}.simple-bidi-website
  (:require
   [clojure.pprint :refer (pprint)]
   [modular.ring :refer (WebRequestHandler)]
   [modular.bidi :refer (as-request-handler)]
   [bidi.bidi :refer (path-for RouteProvider handler)]
   [bidi.ring :refer (redirect)]))

(defrecord Website []
  RouteProvider
  (routes [_]
    ["/" {"index.html"
          (handler ::index (fn [req] {:status 200 :body "Hello, world!"}))
          "" (redirect ::index)}])

  WebRequestHandler
  (request-handler [this] (as-request-handler this)))

(defn new-website []
  (->Website))
