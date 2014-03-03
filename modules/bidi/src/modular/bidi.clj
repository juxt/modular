(ns modular.bidi)

(defprotocol RoutesContributor
  (routes [_])
  (context [_]))
