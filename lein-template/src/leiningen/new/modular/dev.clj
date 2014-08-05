(ns dev
  (:require
   [clojure.pprint :refer (pprint)]
   [clojure.reflect :refer (reflect)]
   [clojure.repl :refer (apropos dir doc find-doc pst source)]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [com.stuartsierra.component :as component]
   [{{name}}.system :refer (config new-base-system-map new-base-dependency-map)]
   [dev-components :refer (new-user-domain-seeder
                           wrap-schema-validation)]
   [modular.wire-up :refer (normalize-dependency-map)]))

(def system nil)

;; We wrap the system in a system wrapper so that we can define a
;; print-method that will avoid recursion.
(defrecord SystemWrapper [p]
  clojure.lang.IDeref
  (deref [this] (deref p))
  clojure.lang.IFn
  (invoke [this a] (p a)))

(defmethod print-method SystemWrapper [_ writer]
  (.write writer "#system \"<system>\""))

(defmethod print-dup SystemWrapper [_ writer]
  (.write writer "#system \"<system>\""))

(. clojure.pprint/simple-dispatch addMethod SystemWrapper
   (fn [x]
     (print-method x *out*)))

(defn new-system-wrapper []
  (->SystemWrapper (promise)))

(defn new-dev-system
  "Create a development system"
  []
  (let [systemref (new-system-wrapper)
        s-map (->
               (new-base-system-map (config) systemref)
               (assoc
                 :user-domain-seeder
                 (component/using
                  (new-user-domain-seeder :users [{:id "dev" :password {{{password}}}}])
                  {:cylon/user-domain :cylon/user-domain})
                 :wrap-schema-validation wrap-schema-validation))
        d-map (merge-with merge
                          (normalize-dependency-map
                           (new-base-dependency-map s-map))
                          (normalize-dependency-map
                           {
                            ;; Here is an example of how to extend the
                            ;; middleware chain using the webhead
                            ;; component. We wire in a dependency on a
                            ;; 1-arity middleware function.
                            :webhead [:wrap-schema-validation]}))]
    (with-meta
      (component/system-using s-map d-map)
      {:dependencies d-map
       :systemref systemref})))

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system
    (constantly (new-dev-system))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root
   #'system
   (fn [system]
     (let [started (component/start system)]
       (deliver (:systemref (meta system)) started)
       started))))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s)))))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start)
  :ok
  )

(defn reset []
  (stop)
  (refresh :after 'dev/go))
