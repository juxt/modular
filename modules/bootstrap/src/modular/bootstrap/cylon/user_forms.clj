;; Copyright © 2014 JUXT LTD.

(ns modular.bootstrap.cylon.user-forms
  (:require
   [com.stuartsierra.component :as component]
   [clojure.tools.logging :refer :all]
   [modular.bootstrap :refer (ContentBoilerplate wrap-content-in-boilerplate)]
   [modular.bidi :refer (path-for)]
   [cylon.impl.authentication :refer (LoginFormRenderer)]
   [cylon.signup.protocols :refer (SignupFormRenderer SimpleMessageRenderer RequestResetPasswordFormRenderer WelcomeRenderer)]
   [cylon.totp :as totp]
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
    [:p.note {:font-size "70%"}]
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

(defrecord BootstrapUserFormRenderer [login-prompt
                                      signup-prompt
                                      reset-password-request-prompt
                                      totp-appname]
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

        [:h2.form-signin-heading login-prompt]

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

        [:p]

        [:div
         (when-let [signup-uri (-> model :form :signup-uri)]
           [:p.note  "Don't have an account? " [:a {:href signup-uri} "Sign up"]])
         (when-let [reset-uri (-> model :form :reset-uri)]
           [:p.note  "Have you forgotten your password? " [:a {:href reset-uri} "Reset"]])]]])))

  SignupFormRenderer
  (render-signup-form
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

        [:h2.form-signin-heading signup-prompt]

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

        [:button.btn.btn-lg.btn-primary.btn-block {:type "submit"} "Sign up"]

        ]])))

  WelcomeRenderer
  (render-welcome
    [this req model]
    (debugf "Model passed to form renderer: %s" model)
    (boilerplate
     this req
     (html
      [:div
       [:style (styles)]
       [:div
        [:p (:name model) ", thank you for signing up. Your user-id is "
         [:tt (:cylon/subject-identifier model)]]

        (when-let [totp-secret (:totp-secret model)]
          [:div
           [:p "Please scan this image into your 2-factor authentication app"]
           [:img {:src (totp/qr-code
                        (format "%s@%s" (:cylon/subject-identifier model)
                                totp-appname)
                        totp-secret)}]
           [:p "Alternatively, type in this secret into your authenticator application: "
            [:code totp-secret]]])
        [:p "We have sent you an email containing a personal verification code. Please check your email and click on the verification link contained in the email."]
        [:div
         [:p "Model..."]
         [:pre (h (pr-str model))]]
        (when (:redirection-uri model) [:p "Now proceed to " [:a {:href (:redirection-uri model)} "continue"]])]])))

  SimpleMessageRenderer
  (render-simple-message [this req model]
    (boilerplate
     this req
     (html
      [:div.row {:style "padding-top: 50px"}
       [:div.col-md-2]
       [:div.col-md-10
        [:h2 (:header model)]]]
      [:div.row
       [:div.col-md-2]
       [:div.col-md-10
        [:style (styles)]
        (:message model)]])))

  RequestResetPasswordFormRenderer
  (render-request-reset-password-form [this req model]
    (boilerplate
     this req
     (html
      [:div
       [:style (styles)]
       [:form.form-signin {:role :form
                           :method (-> model :form :method)
                           :style "border: 1px dotted #555"
                           :action (-> model :form :action)}

        [:h2.form-signin-heading reset-password-request-prompt]
        (when (-> model :reset-status)
            [:div.alert.alert-warning.alert-dismissable
             [:button.close {:type "button" :data-dismiss "alert" :aria-hidden "true"} "&times;"]
             [:span [:strong "Failed: "]
                      (-> model :reset-status)]])

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

        [:button.btn.btn-lg.btn-primary.btn-block {:type "submit"} "Reset Password"]

        ]]))))

(def new-bootstrap-user-form-renderer-schema
  {(s/optional-key :boilerplate) (s/protocol ContentBoilerplate)
   (s/optional-key :totp-appname) s/Str
   :login-prompt s/Str
   :signup-prompt s/Str
   :reset-pw-prompt s/Str})

(defn new-bootstrap-user-form-renderer [& {:as opts}]
  (component/using
   (->> opts
        (merge {:login-prompt "Please sign in&#8230"
                :signup-prompt "Please sign up&#8230"
                :reset-password-request-prompt "Reset your password&#8230"
                :totp-appname "cylon"})
        (s/validate new-bootstrap-user-form-renderer-schema)
        map->BootstrapUserFormRenderer)
   [:boilerplate]))
