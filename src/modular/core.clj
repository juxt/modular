(ns modular.core)

(defn resolve-contributors [m k p & {:keys [cardinality] :or {cardinality :many}} ]
  (let [contributions
        (reduce-kv
         (fn [s _ v]
           (if (satisfies? p v)
             (update-in s [k] conj v)
             s
             ))
         m m)]
    (case cardinality
      :many contributions
      (if (= (count contributions) cardinality)
        contributions
        (throw (ex-info "Contributions didn't match expected cardinality"
                        {:contributions contributions
                         :cardinality cardinality}))))))
