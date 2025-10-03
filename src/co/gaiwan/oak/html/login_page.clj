(ns co.gaiwan.oak.html.login-page
  (:require
   [co.gaiwan.oak.html.tokens :refer :all]
   [co.gaiwan.oak.lib.form :as form]
   [lambdaisland.ornament :as o]))

(o/defstyled login-html :main
  [:>div
   {:display "flex"
    :margin "0 auto"
    :flex-direction "column"
    :max-width --size-15
    :background-color --panel-bg
    :color --panel-text}]
  ([]
   [:div
    [:h1 "Login"]
    [form/form {:method "POST"}
     [:div.form-group
      [:label {:for "email"} "Email address"]
      [:input#email
       {:type "email"
        :name "email"
        :required "required"
        :autocomplete "username"
        :placeholder "you@example.com"
        :aria-describedby "email-help"}]
      [:small#email-help.form-help-text
       "Enter the email associated with your account."]]
     [:div.form-group
      [:label {:for "password"} "Password"]
      [:input#password
       {:type "password"
        :name "password"
        :required "required"
        :autocomplete "current-password"
        :placeholder "Enter your password"}]]
     [:button {:type "submit"} "Log In"]]]))
