(ns {{name}}.templated-bidi-website
  (:require
   [clojure.pprint :refer (pprint)]
   [modular.ring :refer (WebRequestHandler)]
   [modular.bidi :refer (WebService as-request-handler)]
   [bidi.bidi :refer (path-for ->Redirect)]))

(defrecord Website []
  WebService
  (request-handlers [this]
    {::index (fn [req] {:status 200 :body "Hello, world!"})})

  (routes [_] ["/" {"index.html" ::index
                    "" (->Redirect 307 ::index)}])

  (uri-context [_] "")

  WebRequestHandler
  (request-handler [this] (as-request-handler this)))

(defn new-website []
  (->Website))
