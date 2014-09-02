;; Copyright © 2014 JUXT LTD.

(ns modular.bootstrap.cylon.login-form
  (:require
   [clojure.tools.logging :refer :all]
   [modular.bootstrap :refer (ContentBoilerplate wrap-content-in-boilerplate)]
   [cylon.impl.authentication :refer (LoginFormRenderer)]
   [hiccup.core :refer (html h)]
   [garden.core :refer (css)]
   [garden.units :refer (pt em px)]
   [garden.color :refer (rgb)]
   [schema.core :as s]))

(defn styles
  "From http://getbootstrap.com/examples/signin/signin.css"
  []
  (css
   [:.form-signin {:max-width (px 330)
                   :padding (px 15)
                   :margin "40px auto"}
    [:.form-signin-heading :.checkbox {:margin-bottom (px 10)}]
    [:.checkbox [:font-weight :normal]]
    [:.form-control {:position :relative
                     :height :auto
                     :box-sizing :border-box
                     :padding (px 10)
                     :font-size (px 16)}]
    [:.form-control:focus {:z-index 2}]
    ["input[type=\"email\"]" {:margin-bottom (px -1)
                              :border-bottom-right-radius 0
                              :border-bottom-left-radius 0}]
    ["input[type=\"password\"]" {:margin-bottom (px 10)
                                 :border-bottom-right-radius 0
                                 :border-bottom-left-radius 0}]]))

(defn boilerplate [component req content]
  (if-let [bp (:boilerplate component)]
    (wrap-content-in-boilerplate bp req content)
    content))

(defrecord BootstrapLoginFormRenderer [prompt]
  LoginFormRenderer
  (render-login-form
    [this req model]
    (debugf "Model passed to form renderer: %s" model)
    (boilerplate
     this req
     (html
      [:div
       [:style (styles)]
       [:form.form-signin {:role :form
                           :method (-> model :form :method)
                           :style "border: 1px dotted #555"
                           :action (-> model :form :action)}

        [:h2.form-signin-heading prompt]

        #_(when login-status
            [:div.alert.alert-warning.alert-dismissable
             [:button.close {:type "button" :data-dismiss "alert" :aria-hidden "true"} "&times;"]
             (case login-status
               :failed [:span [:strong "Failed: "] "Please check email and password and try again or " [:a.alert-link {:href "#"} "reset your password"] "."])])

        (for [[n {:keys [name password? placeholder required autofocus value]}]
              (map vector (range) (-> model :form :fields))]
          [:input.form-control
           (merge
            {:name name
             :type (if password? "password" "text")
             :value value}
            (when placeholder {:placeholder placeholder})
            (when required {:required required})
            (when autofocus {:autofocus autofocus}))])

        #_[:label.checkbox
         [:input {:name "remember" :type :checkbox :value "remember-me"} "Remember me"]]

        [:button.btn.btn-lg.btn-primary.btn-block {:type "submit"} "Sign in"]

        #_[:p]
        #_[:a {:href "#"} "Reset password"]
        ]]))))

(def new-bootstrap-login-form-renderer-schema
  {(s/optional-key :boilerplate) (s/protocol ContentBoilerplate)
   :prompt s/Str})

(defn new-bootstrap-login-form-renderer [& {:as opts}]
  (->> opts
       (merge {:prompt "Please sign in&#8230"})
       (s/validate new-bootstrap-login-form-renderer-schema)
       map->BootstrapLoginFormRenderer))
