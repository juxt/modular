;; Copyright © 2014 JUXT LTD.

(ns modular.ring
  (:require
   [modular.core :as mod]))

(defprotocol RingHandlerProvider
  (handler [_]))
