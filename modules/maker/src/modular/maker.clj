;; Copyright © 2014 JUXT LTD.

(ns modular.maker)

(defn- make-args
  "In modular, constructors use the variadic keyword arguments
  call-convention. This function allows us to formulate these arguments
  from a config map and a list of specified keys. Each key can take a
  default value, or nil if no value should be passed. The value will
  then be determined by the constructor itself, not the calling code."
  [cfg & {:as args}]
  (as-> args %
        (merge % cfg)
        (select-keys % (keys args))
        (seq %)
        (remove (comp nil? second) %)
        (apply concat %)))

(defn make
  "Call the constructor with default keyword arguments, each of which is
   overridden if the entry exists in the given config map."
  ([ctr cfg & kvs]
     (assert fn? ctr)
     (assert (not (keyword? cfg)) "Please specify a config map as the second argument to make")
     (apply ctr (apply make-args cfg kvs)))
  ;; If only the constructor is specified, do the sensible thing.
  ([ctr]
     (make ctr {})))
