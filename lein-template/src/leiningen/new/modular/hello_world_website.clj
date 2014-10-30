(ns {{name}}.hello-world-website
  (:require
   [modular.ring :refer (WebRequestHandler)]))

(defrecord HelloWorldHandler []
  WebRequestHandler
  (request-handler [this]
    (fn [req] {:status 200 :body "Hello, world!"})))

(defn new-handler []
  (->HelloWorldHandler))
