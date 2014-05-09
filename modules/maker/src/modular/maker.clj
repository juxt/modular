;; Copyright © 2014 JUXT LTD.

(ns modular.maker)

(defn make-args
  "In modular, constructors use the variadic keyword arguments
call-convention. This function allows us to formulate these arguments
from a config map and a list of specified keys. Each key can take a
default value, which can be nil, in which case no value is passed and
the constructor will determine the value, unless the key exists in the
config. If the value is the special value of :modular.maker/required, an
exception will be thrown if no value can be found in the config map.

 In cases where there is no a correspondence between the keyword
argument required by the constructor and the config map, a mapping
can be given. Instead of using a keyword for the key, a map with a
single kv entry can be given. The key is the one expected by the
constructor. The value can be key or vector of keys, representing the
path in the config where the value should be found.

See modular.maker-tests/make-args-test for examples."
  [cfg & args]
  (apply concat
         (for [[k dv]
               (partition 2 args)]
           (let [v (get-in cfg
                           (cond (keyword? k) [k]
                                 (associative? k)
                                 (let [path (second (first (seq k)))]
                                   (cond (keyword? path) [path]
                                         (vector? path) path))))]
             (cond
              (and (= dv :modular.maker/required) (nil? v))
              (throw (ex-info "Configuration value required but couldn't be found" {:key-or-mapping k}))
              (keyword? k) [k (or v dv)]
              (associative? k) [(ffirst (seq k)) (or v dv)])))))

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
