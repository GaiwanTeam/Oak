(ns co.gaiwan.oak.html.login-page
  (:require
   [co.gaiwan.oak.html.forms :as f]
   [co.gaiwan.oak.html.tokens :refer :all]
   [co.gaiwan.oak.html.widgets :as w]
   [co.gaiwan.oak.util.routing :as routing]
   [lambdaisland.ornament :as o]))

(o/defstyled input-group :div
  {:margin-bottom --size-4}
  [:label
   {:display       "block"
    :margin-bottom --size-2
    :font-weight   :bold}]
  [:input
   {:width "100%"}]
  ([props]
   [:<>
    [:label {:for (:id props)} (:label props)]
    [:input props]]))

(o/defstyled forgot-password-link :a.subtle
  {:text-align :right}
  ([req]
   [:<> {:href (routing/url-for req :auth/password-reset)} "Forgot password?"]))

(o/defstyled login-html :main
  {:display         :flex
   :justify-content :center
   :align-items     :center
   :min-height      "100vh"}
  ([req]
   [:<>
    [w/leaf-bg]
    [w/full-center-card
     [:h1 "Login"]
     [f/form {:method "POST"}
      [f/input-group
       {:label        "Email address"
        :id           "email"
        :type         "email"
        :name         "email"
        :required     "required"
        :autocomplete "username"
        :placeholder  "you@example.com"
        :autofocus    "autofocus"}]
      [f/input-group
       {:id           "password"
        :label        "Password"
        :type         "password"
        :name         "password"
        :required     "required"
        :autocomplete "current-password"
        :placeholder  "Enter your password"}]
      [f/submit {:type "submit" :value "Log In"}]]
     [forgot-password-link req]]]))
