(ns modular.core)

(defn resolve-contributors [m k p & {:keys [cardinality] :or {cardinality :many}}]
  (let [contributions (keep (fn [[_ v]] (when (satisfies? p v) v)) m)]
    (assoc m k (case cardinality
                 :many contributions
                 (if (= (count contributions) cardinality)
                   (if (= 1 cardinality) (first contributions) contributions)
                   (throw (ex-info "Contributions didn't match expected cardinality"
                                   {:contributions contributions
                                    :cardinality cardinality})))))))
