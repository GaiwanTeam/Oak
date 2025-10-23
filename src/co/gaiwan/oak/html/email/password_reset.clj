(ns co.gaiwan.oak.html.email.password-reset
  (:require
   [co.gaiwan.oak.html.tokens :as t]
   [clojure.string :as str]))

(defn reset-html [{:keys [name org-name reset-link expiry-hours]}]
  [:html {:lang "en"}
   [:head [:meta {:charset "utf-8"}] [:title "Password Reset"]]
   [:body
    {:style {:margin      0
             :padding     "20px"
             :font-family "Arial,Helvetica,sans-serif"}}
    [:p "Hi " name ","]
    [:p
     "We received a request to reset your password for your " org-name " account."]
    [:p
     [:a
      {:href  reset-link
       :style {:display         :inline-block
               :padding         "10px 16px"
               :background      (t/prop-default-val t/--bg-call-to-action)
               :color           (t/prop-default-val t/--text-call-to-action)
               :text-decoration :none
               :border-radius   (t/prop-default-val t/--radius-2)}}
      "Reset your password"]]
    [:p
     "If that button doesn’t work, copy and paste this link into your browser:"
     [:br]
     [:a {:href reset-link} reset-link]]
    [:p "This link will expire in " expiry-hours " hours."]
    [:p
     "If you didn’t request a password reset, you can safely ignore this email."]]])

(comment
  {:name "Arne"
   :org-name (co.gaiwan.oak.app.config/get :org/name)
   :reset-link "http://example.com"
   :expiry-hours 4})
