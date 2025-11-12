(ns co.gaiwan.oak.html.dashboard
  (:require
   [co.gaiwan.oak.html.forms :as f]
   [co.gaiwan.oak.html.tokens :refer :all]
   [co.gaiwan.oak.html.widgets :as w]
   [co.gaiwan.oak.html.forms :as form]
   [co.gaiwan.oak.lib.time :as time]
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

(o/defstyled security-actions :div
  {:display :flex
   :gap "1rem"})

(o/defstyled oauth-apps :div
  {:margin-top "1rem"}
  [:>div {:margin 0
          :padding 0}]
  [:.app-item {:display :flex
               :justify-content :space-between
               :align-items :center
               :padding ["1rem" 0]
               :border-bottom-width "1px"
               :border-bottom-style :solid
               :border-bottom-color --gray-2}]
  [:.app-info {:display :flex
               :align-items :center
               :gap "1rem"}
   [:h4 {:margin-top 0
         :margin-bottom 0}]
   [:p {:margin-top 0
        :margin-bottom 0}]]
  [:.app-icon {:display :flex
               :justify-content :center
               :align-items :center
               :width "2rem"
               :height "2rem"
               :border-radius "0.4rem"
               :background-color --oak-green-3
               :font-weight :bold
               :color --oak-green-9}])

(defn dash-page [{:keys [req debug? authorized-apps totp-setup-url logout-url]}]
  (prn :debug-apps authorized-apps)
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
     [security-actions
      [:button.cautious-action "Disable 2FA"]
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
     [oauth-apps
      (for [app authorized-apps]
        (let [app-name (:oauth-client/client-name app)
              auth-id (:oauth-authorization/id app)
              authorized-time (:oauth-authorization/updated-at app)]
          [:div.app-item
           [:div.app-info
            [:div.app-icon  "AS"]
            [:div [:h4 app-name]
             [:p (str "Authorized on " (time/format-date authorized-time))]]]
           [f/form {:method "POST" :action "/dashboard/auth-apps"}
            [f/input-group {:type "hidden"
                            :name "auth-id"
                            :value auth-id}]
            [f/submit-delete {:type "submit" :value "Remove"}]]]))]]
    (when debug?
      [w/column-card
       [:h1 "Debug"]
       [:a {:href totp-setup-url} "Setup 2FA"]
       [:a {:href logout-url} "Sign out"]
       [:p (pr-str (:identity req))]])]])
