;; Copyright © 2014, JUXT LTD. All Rights Reserved.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

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
