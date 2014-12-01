(defn routes []
  (-> system :modular-bidi-router-webrouter :routes))

(defn path->route [path]
  (match-route (routes) path))
