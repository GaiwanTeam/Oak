(ns co.gaiwan.oak.html.dashboard
  (:require
   [co.gaiwan.oak.html.forms :as f]
   [co.gaiwan.oak.html.tokens :refer :all]
   [co.gaiwan.oak.html.widgets :as w]
   [co.gaiwan.oak.html.forms :as form]
   [lambdaisland.ornament :as o]))

(o/defstyled dash-layout :div
  {:display         :flex
   :justify-content :center
   :align-items     :center
   :min-height      "100vh"}
  [:p
   {:text-align :center
    :margin-top 0}]
  ([& children]
   [:<>
    [w/leaf-bg]
    (into [w/full-center-card] children)]))

(defn dash-page [{:keys [req totp-setup-url logout-url]}]
  [dash-layout
   [:p (pr-str (:identity req))]
   [:a {:href totp-setup-url} "Setup 2FA"]
   [:a {:href logout-url} "Sign out"]])
