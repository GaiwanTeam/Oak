(ns co.gaiwan.oak.html.dashboard
  (:require
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.html.layout :as layout]
   [co.gaiwan.oak.html.forms :as f]
   [co.gaiwan.oak.html.tokens :refer :all]
   [co.gaiwan.oak.html.widgets :as w]
   [co.gaiwan.oak.html.graphics :as g]
   [co.gaiwan.oak.html.forms :as form]
   [co.gaiwan.oak.lib.time :as time]
   [co.gaiwan.oak.util.routing :as routing]
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

(o/defstyled logout :div
  [:a {:display :flex
       :align-items :center
       :text-align :center}]
  ([uri]
   [:a {:href uri}
    [g/logout]
    [:span "Logout"]]))

(o/defstyled dash-header w/column-card
  {:margin-bottom "2rem"
   --bg-panel "rgba(255,255,255,20%)"
   --text-heading --oak-green-13
   :padding [--size-2]}
  [:>div {:display :flex
          :align-items :center
          :gap "1rem"}]
  [:.user-details {:margin 0
                   :padding 0}]
  [:.push-right {:margin-right "auto"}]
  [:h2 {:margin-top 0
        :margin-bottom "0.25rem"
        :padding 0}]
  [:p {:color --gray-8
       :margin 0
       :padding 0}]
  ([{:keys [username email]} uri]
   [:div.user-info
    [w/avatar]
    [:div.user-details.push-right
     (if username
       [:<>
        [:h2 username]
        [:p email]]
       [:h2 email])]
    [logout uri]]))

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
               :background-color --oak-green-3}
   [:img
    {:max-width "100%"
     :height :auto}]]
  [:.app-text-icon {:display :flex
                    :justify-content :center
                    :align-items :center
                    :width "2rem"
                    :height "2rem"
                    :border-radius "0.4rem"
                    :background-color --oak-green-3
                    :font-weight :bold
                    :color --oak-green-9}])

(defn dash-page [{:keys [req identifiers debug? enable-2fa? authorized-apps totp-setup-url logout-url
                         params password-error new-password-error confirm-error]}]
  [dash-layout
   (let [{:keys [error success]} (:flash req)]
     [:<>
      (when error [layout/flash-error error])
      (when success [layout/flash-success success])])
   [dash-header identifiers logout-url]
   [card-container
    [w/column-card
     [:h3 "Account Security"]
     [security-header
      [:div.status-indicator (if enable-2fa?
                               {:class "status-active"}
                               {:class "status-inactive"})]
      [:span (str "Two-Factor Authentication: " (if enable-2fa?
                                                  "Active"
                                                  "Inactive"))]]
     [:p "Enhance your account security with two-factor authentication."]
     [security-actions
      [f/form {:method "POST" :action "/dashboard/2fa"}
       [f/input-group {:type "hidden"
                       :name "enable-2fa"
                       :value (str (not enable-2fa?))}]
       (if enable-2fa?
         [f/submit-disable {:type "submit" :value "Disable 2FA"}]
         [f/submit-disable {:type "submit" :value "Enable 2FA"}])]]]
    [w/column-card
     [:h3 "Change Password"]
     [f/form {:method "POST" :action "/dashboard/password"}
      [f/input-group {:label "Current Password"
                      :id "current-password"
                      :type "password"
                      :name "current-password"
                      :required "required"
                      :error password-error}]
      [f/input-group {:label "New Password"
                      :id "new-password"
                      :type "password"
                      :name "new-password"
                      :required "required"
                      :error new-password-error}]
      [f/input-group {:label "Confirm New Password"
                      :id "confirm-new-password"
                      :type "password"
                      :name "confirm-new-password"
                      :required "required"
                      :error confirm-error}]
      [f/submit {:type "submit" :value "Update Password"}]]]
    [w/column-card
     [:h3 "Authorized Applications"]
     [:p "These applications have access to your account."]
     [oauth-apps
      (for [app authorized-apps]
        (let [app-name (:oauth-client/client-name app)
              app-id  (:oauth-client/client-id app)
              app-icon-uri (:oauth-client/logo-uri app)
              app-uri (:oauth-client/client-uri app)
              auth-id (:oauth-authorization/id app)
              authorized-time (:oauth-authorization/updated-at app)]
          [:div.app-item
           [:div.app-info
            (if app-icon-uri
              [:div.app-icon [:a {:href app-uri}
                              [:img {:src (routing/url-for req :proxy/image {:client-id app-id})}]]]
              [:div.app-text-icon (first app-name)])
            [:div [:h4 app-name]
             [:p (str "Authorized on " (time/format-date authorized-time))]]]
           [f/form {:method "delete" :action "/authorizations/app"
                    :data-confirm "Are you sure you want to remove this item? This action will permanently delete the resource."}
            [f/input-group {:type "hidden"
                            :name "auth-id"
                            :value auth-id}]
            [f/submit-delete {:type "submit" :value "Remove"}]]]))]]
    (when (and debug? (config/get :dev/allow-debug?))
      [w/column-card
       [:h1 "Debug"]
       [:a {:href totp-setup-url} "Setup 2FA"]
       [:a {:href logout-url} "Sign out"]
       [:p (pr-str (:identity req))]])]])
