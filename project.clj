;; Copyright © 2014 JUXT LTD.

; The version here doesn't really matter because it's the subprojects
; we're interested in
(defproject juxt/modular "0.5.1"
  :description "FIXME: write description"
  :url "http://github.com/mastodonc/kixi.stentor"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :plugins [[lein-sub "0.2.3"]]

  :sub [
        ;; Uncomment these when they need a new release
        "modules/ring"
        "modules/http-kit"
        "modules/bidi"
        ;; "modules/cassandra"
        "modules/datomic"

        "modules/netty"
        "modules/mqtt"

        "modules/template"
        "modules/web-template"
        "modules/cljs"

        ;; "modules/menu"
        "modules/maker"
        "modules/wire-up"

        ;; New modules to be released (snapshots)
        "modules/http-kit-events"
        "modules/async"
        "modules/menu"

        ;; Modules that still need to be ported over from Jig 1.x
        ;;        "modules/async" ; core.async channels that can be shared by dependants
        ;;        "modules/compojure" ; URI routing library
        ;;        "modules/jetty" ; HTTP server
        ]

  :dependencies [[org.clojure/tools.logging "0.2.6"]]

  :repl-options {:prompt (fn [ns] (str "modular " ns "> "))}

  :aliases {"deploy-all" ["sub" "deploy" "clojars"]
            "install-all" ["sub" "install"]})
