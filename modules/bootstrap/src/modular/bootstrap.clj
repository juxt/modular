(ns modular.bootstrap)

(defprotocol ContentBoilerplate
  (wrap-content-in-boilerplate [_ req content]))
