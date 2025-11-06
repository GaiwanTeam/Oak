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

(o/defstyled security-header :div
  {:display :flex
   :align-items :center
   :gap "0.5rem"
   :margin-bottom "1rem"})

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
     [security-header
      [:div.status-indicator {:class "status-active"}]
      [:span "Two-Factor Authentication: Active"]]
     [:p "Enhance your account security with two-factor authentication."]
     [:div
      {:class "action-buttons"}
      [:button.call-to-action  "Disable 2FA"]
      [:button.call-to-action "Change Settings"]]]

    [w/column-card
     [:h3 "Change Password"]
     [f/form {:method "POST"}
      [f/input-group {:label "Current Password"
                      :id "current-password"
                      :type "password"
                      :name "current-password"
                      :required "required"}]
      [f/input-group {:label "New Password"
                      :id "new-password"
                      :type "password"
                      :name "new-password"
                      :required "required"}]
      [f/input-group {:label "Confirm New Password"
                      :id "confirm-new-password"
                      :type "password"
                      :name "confirm-new-password"
                      :required "required"}]
      [f/password-validate-script]
      [f/submit {:type "submit" :value "Update Password"}]]]
    [w/column-card
     [:h3 "Authorized Applications"]
     [:p "These applications have access to your account."]
     [:div.oauth-apps
      [:div.app-item
       [:div.app-info
        [:div.app-info  "AS"]
        [:div [:h4 "Analytics Suite"] [:p "Authorized on Sep 22, 2023"]]]
       [:button.call-to-action  "Remove"]]
      [:div.app-item
       [:div.app-info
        [:div.app-icon  "CD"]
        [:div [:h4 "Cloud Drive"] [:p "Authorized on Aug 5, 2023"]]]
       [:button.call-to-action  "Remove"]]]]
    (when debug?
      [w/column-card
       [:h1 "Debug"]
       [:a {:href totp-setup-url} "Setup 2FA"]
       [:a {:href logout-url} "Sign out"]
       [:p (pr-str (:identity req))]])]])
