(ns co.gaiwan.oak.html.password-reset
  (:require
   [co.gaiwan.oak.html.forms :as f]
   [co.gaiwan.oak.html.graphics :as g]
   [co.gaiwan.oak.html.tokens :as t]
   [co.gaiwan.oak.html.widgets :as w]
   [co.gaiwan.oak.util.routing :as routing]
   [lambdaisland.ornament :as o]))

(o/defstyled layout :main
  {:display         :flex
   :justify-content :center
   :align-items     :center
   :min-height      "100vh"}
  ([& children]
   [:<>
    [w/leaf-bg]
    (into [w/full-center-card] children)]))

(defn password-reset-html [req]
  [layout
   [:h1 "Reset Password"]
   [:p "Enter your email and we'll send you a link to reset your password."]
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
    [f/submit {:type "submit" :value "Send Reset Link"}]]
   [:a.subtle {:href (routing/url-for req :auth/login)} "Back to login"]])

(o/defstyled password-reset-requested :div
  {:text-align :center
   w/--card-width t/--size-fluid-11}
  [:.email {:text-decoration :underline}]
  [g/envelope {:align-self :center
               :width "4rem"}
   [:path {:stroke t/--text-subtle}]]
  ([req {:keys [email expiry-hours]}]
   [layout
    [:h1 "Check your email"]
    [:p "We've sent a " [:strong "password reset link to " [:span.email email]] ", if an account exists with that email."]
    [:p
     "Please check your inbox and click the link to set a new password."]
    [:p
     "The link is valid for " expiry-hours " hours."]
    [g/envelope]
    [:a.subtle {:href (routing/url-for req :auth/login)} "Back to login"]]))

(o/defstyled password-reset-form :div
  ([req {:keys [email password confirm-password minlength
                password-error confirm-error]}]
   [layout
    [:h1 "Reset Password"]
    [:p "Choose a new password for " [:strong email]]
    [f/form {:method "POST"}
     [f/input-group
      {:id           "password"
       :label        "Password"
       :type         "password"
       :name         "password"
       :required     "required"
       :autocomplete "new-password"
       :placeholder  "Enter new password"
       :error        password-error
       :value        password
       :minlength    minlength
       :autofocus    "autofocus"}]

     [f/input-group
      {:id           "confirm-password"
       :label        "Confirm Password"
       :type         "password"
       :name         "confirm-password"
       :required     "required"
       :autocomplete "new-password"
       :placeholder  "Repeat new password"
       :error        confirm-error
       :value        confirm-password
       :minlength    minlength}]
     [f/submit {:type "submit" :value "Reset password"}]]]))

(o/defstyled password-reset-success :div
  {:text-align :center}
  [g/checkmark {:height "3rem"
                :margin "1rem"
                :align-self :center}]
  ([req]
   [layout
    [:h1 "Password Successfully Reset"]
    [:p "Your password has been updated. You can now log in with your new password."]
    [g/checkmark]
    [:a.subtle {:href (routing/url-for req :auth/login)} "Back to Login"]]))


(o/defstyled error-page :div
  {:text-align :center}
  [g/error-cross {:height "3rem"
                  :margin "1rem"
                  :align-self :center}]
  ([req message]
   [layout
    [:h1 "Request Failed"]
    [:p message]
    [g/error-cross]
    [:a.subtle {:href (routing/url-for req :auth/login)} "Back to Login"]]))
