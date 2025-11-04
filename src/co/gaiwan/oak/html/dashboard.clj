(ns co.gaiwan.oak.html.dashboard
  (:require
   [co.gaiwan.oak.html.forms :as f]
   [co.gaiwan.oak.html.tokens :refer :all]
   [co.gaiwan.oak.html.widgets :as w]
   [co.gaiwan.oak.html.forms :as form]
   [lambdaisland.ornament :as o]))

(o/defstyled dash-layout :div
  ([& children]
   (into
    [:<>
     [w/leaf-bg]
     children])))

(o/defstyled card-container :div
  {:display :grid
   :grid-template-columns "repeat( auto-fit, minmax(40em, 1fr) )"
   :marging "1rem"
   :gap "1rem"})

(defn dash-page [{:keys [req totp-setup-url logout-url]}]
  [dash-layout
   [card-container
    [w/card
     [:h1 "Account Security"]
     [:a {:href totp-setup-url} "Setup 2FA"]
     [:a {:href logout-url} "Sign out"]]
    [w/card
     [:h1 "Change Password"]]
    [w/card
     [:h1 "Authentications"]]
    [w/card
     [:h1 "Debug"]
     [:p (pr-str (:identity req))]]]
   ])
