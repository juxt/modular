(ns {{name}}.core
    (:require
     [om.core :as om :include-macros true]
     [sablono.core :as html :refer-macros [html]]))

(enable-console-print!)

(println "{{name}}.core loaded")

(def model
  (atom
   {:sidebar
    [{:label "Overview"}
     {:label "Reports"}
     {:label "Analytics"}
     {:label "Export"}]}))

(defn dashboard [app owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:p "dashboard rendered!"]))))


(defn ^:export main []
  (println "Calling main!")
  (om/root dashboard model {:target (.getElementById js/document "content")}))
