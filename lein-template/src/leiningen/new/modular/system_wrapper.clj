(ns {{name}}.system-wrapper)

(defrecord SystemWrapper [p]
  clojure.lang.IDeref
  (deref [this] (deref p))
  clojure.lang.IFn
  (invoke [this a] (p a))
  )


(defmethod print-method SystemWrapper [_ writer]
  (.write writer "#system \"<system>\"")

)

(let [sw (SystemWrapper. (promise))]
  (deliver sw :foo)
  (println @sw)
  (println sw)
  )
