(require 'bidi.bidi
         'modular.bidi)

(defn routes []
  (-> system :modular-bidi-router-webrouter :routes))

(defn match-route [path]
  (bidi.bidi/match-route (routes) path))

(defn path-for [path & args]
  (apply modular.bidi/path-for
         (-> system :modular-bidi-router-webrouter) path args))
