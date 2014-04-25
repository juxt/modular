;; Copyright © 2014 JUXT LTD.

(ns modular.bidi-tests
  (:require
   [clojure.test :refer :all]
   [modular.bidi :refer (WebService ring-handler-map uri-context routes ->ComponentAddressable ->Router)]
   [bidi.bidi :as bidi :refer (match-route path-for)]
   [com.stuartsierra.component :as component])
  )

(deftest web-service-components
  (let [component-A
        (reify WebService
          (ring-handler-map [_] {:a "Alpha"
                                 :b "Beta"})
          (routes [_] ["/" {["a/" :docid] :a
                            "b" :b}])
          (uri-context [_] ""))

        component-B
        (reify WebService
          (ring-handler-map [_] {:a "Delta"
                                 :b "Gamma"})
          (routes [_] ["/" {"a" :a
                            "b" :b}])
          (uri-context [_] "/g"))

        system (-> (component/system-map
                    :router (->Router)
                    :ws-a component-A
                    :ws-b component-B)
                   (component/system-using
                    {:router [:ws-a :ws-b]})
                   component/start)

        router (:router system)
        routes (:routes router)]

    (is router)
    (is (:routes router))

    (is (= (match-route routes "/a/123") {:handler "Alpha", :params {:docid "123"}}))
    (is (= (match-route routes "/b") {:handler "Beta"}))
    (is (= (match-route routes "/g/a") {:handler "Delta"}))
    (is (= (match-route routes "/g/b") {:handler "Gamma"}))

    (is (= (path-for routes [:ws-a :a] :docid "123") "/a/123"))
    (is (= (path-for routes [:ws-a :b]) "/b"))
    (is (= (path-for routes [:ws-b :a]) "/g/a"))
    (is (= (path-for routes [:ws-b :b]) "/g/b"))))

;; TODO Turn this into a test for component preferencing keys
#_(require '[ring.mock.request :refer (request)])
#_(let [
      component-preference
      (fn [matched ckey]
        (reify
          bidi/Matched
          (resolve-handler [this m]
            (bidi/resolve-handler matched m))
          (unresolve-handler [this m]
            (if (keyword? (:handler m))
              (or
               ;; In case there's another component using the same key in a handler-map,
               ;; preference a path to an 'internal' handler first.
               (bidi/unresolve-handler matched (assoc m :handler [ckey (:handler m)]))
               (bidi/unresolve-handler matched m))
              (bidi/unresolve-handler matched m)))))

      wrap-handler
      (fn [h ckey] (if (fn? h)
                     (fn [req]
                       (h (update-in req [:routes] (fn [r] ["" (component-preference [r] ckey)]))))
                     h))
      c-matched
      (fn [matched ckey handlers]
        (reify bidi/Matched
          (resolve-handler [this m]
            (when-let [{:keys [handler] :as res} (bidi/resolve-handler matched m)]
              (assoc res :handler (wrap-handler (get-in handlers [ckey handler]) ckey))))

          (unresolve-handler [this m]
            (cond (vector? (:handler m))
                  (when (= ckey (first (:handler m)))
                    (bidi/unresolve-handler matched (update-in m [:handler] last)))
                  :otherwise (bidi/unresolve-handler matched m)
                  ))))

      handler-a-a (fn [{routes :routes}]
                    {:body (str "Alpha "
                                (path-for routes [:B :g])
                                " "
                                (path-for routes [:B :a])
                                " "
                                (path-for routes :a :docid 100)
                                " "
                                (path-for routes :z))})

      handler-b-g (fn [{routes :routes}]
                    {:body (str "Gamma "
                                (path-for routes :a)
                                )})

      handler-c-a (fn [{routes :routes}]
                    {:body (str "C "
                                (path-for routes :a)
                                )})

      component-A
      (reify WebService
        (ring-handler-map [_] {:a handler-a-a
                               :b "Beta"})
        (routes [_] ["/" {["a/" :docid] :a
                          "b" :b}])
        (uri-context [_] ""))

      component-B
      (reify WebService
        (ring-handler-map [_] {:a "Delta"
                               :g handler-b-g})
        (routes [_] ["/" {"a" :a
                          "g" :g}])
        (uri-context [_] "/g"))

      component-C
      (reify WebService
        (ring-handler-map [_] {:a handler-c-a
                               :z "Zigzag"})

        (routes [_] ["/" {"a" :a
                          "z" :z}])

        (uri-context [_] "/C"))

      deps {:A component-A
            :B component-B
            :C component-C}

      handlers (apply merge
                      (for [[k v] deps
                            :when (satisfies? WebService v)]
                        {k (ring-handler-map v)}))

      this {:handlers handlers
            :routes ["" (vec (for [[k v] (sort deps)
                                   :when (satisfies? WebService v)]
                               [(or (uri-context v) "")
                                (c-matched [(routes v)] k handlers)]))]}]

  (assert (= ((:handler (match-route (:routes this) "/a/13"))
              (assoc (request :get "/a/13") :routes (:routes this)))
             {:body "Alpha /g/g /g/a /a/100 /C/z"}))

  (assert (= (match-route (:routes this) "/b") {:handler "Beta"}))
  (assert (= (match-route (:routes this) "/g/a") {:handler "Delta"}))

  (assert (= ((:handler (match-route (:routes this) "/C/a"))
              (assoc (request :get "/C/a") :routes (:routes this)))
             {:body "C /C/a"}))

  (assert (= (path-for (:routes this) [:A :a] :docid "123") "/a/123"))
  (assert (= (path-for (:routes this) [:A :b]) "/b"))
  (assert (= (path-for (:routes this) [:B :a]) "/g/a"))
  (assert (= (path-for (:routes this) [:B :g]) "/g/g"))





  )
