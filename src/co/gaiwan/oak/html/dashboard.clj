(ns co.gaiwan.oak.html.dashboard
  (:require
   [co.gaiwan.oak.html.forms :as f]
   [co.gaiwan.oak.html.tokens :refer :all]
   [co.gaiwan.oak.html.widgets :as w]
   [co.gaiwan.oak.html.forms :as form]
   [lambdaisland.ornament :as o]))

(o/defstyled dash-layout :div
  {:display :flex
   :flex-direction :column
   :padding "2rem"}
  ([& children]
   (into
    [:<>
     [w/leaf-bg]
     children])))

(o/defstyled card-container :div
  {:display :grid
   :grid-template-columns "repeat( auto-fit, minmax(20rem, 1fr) )"
   :gap "1.5rem"})

(o/defstyled dash-header :div
  {:margin-bottom "2rem"}
  [:>div {:display :flex
          :align-items :center
          :gap "1rem"}]
  [:.user-details {:margin 0
                   :padding 0}]
  [:h2 {:margin-top 0
        :margin-bottom "0.25rem"
        :padding 0}]
  [:p {:color --gray-8
       :margin 0
       :padding 0}])

(defn dash-page [{:keys [req debug? totp-setup-url logout-url]}]
  [dash-layout
   [dash-header
    [:div.user-info
     [w/avatar]
     [:div.user-details
      [:h2 "John Doe"]
      [:p "john.doe@example.com"]]]]
   [card-container
    [w/column-card
     [:h3 "Account Security"]
     [:a {:href totp-setup-url} "Setup 2FA"]
     [:a {:href logout-url} "Sign out"]]
    [w/column-card
     [:h3 "Change Password"]]
    [w/column-card
     [:h3 "Authorized Applications"]]
    (when debug?
      [w/column-card
       [:h1 "Debug"]
       [:p (pr-str (:identity req))]])]])
