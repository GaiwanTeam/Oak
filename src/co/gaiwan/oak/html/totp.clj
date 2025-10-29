(ns co.gaiwan.oak.html.totp
  (:require
   [co.gaiwan.oak.html.forms :as f]
   [co.gaiwan.oak.html.widgets :as w]
   [co.gaiwan.oak.util.routing :as routing]
   [lambdaisland.ornament :as o]))

(o/defstyled totp-layout :div
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


(defn success-page [req {:keys [cred-save-success?]}]
  [totp-layout
   [:<>
    [:h1 "2FA Enabled"]
    [:div "Your authenticator app has been successfully linked."]
    [:p [:a.subtle {:href (routing/url-for req :auth/login)} "Back to login"]]]])

(o/defstyled qr-img :img
  {:max-width "80%"
   :margin "0 auto"})

(defn setup-page [{:keys [data-uri next-uri]}]
  [totp-layout
   [:h1 "Set up 2FA"]
   [:p#totp-desc "Use a 2FA app to scan the QR code, then provide the 6-digit code it generates."]
   [qr-img {:src data-uri}]
   [f/form {:method "POST"}
    [f/input-group
     {:label            "2FA code from authenticator app"
      :id               "code"
      :type             "text"
      :name             "code"
      :required         "required"
      :aria-describedby "totp-desc"
      :autofocus        "autofocus"}]
    [f/submit {:type "submit" :value "Enable 2FA"}]]])

(defn check-page [{:keys [error]}]
  [totp-layout
   [:h1 "Verify 2FA"]
   [:p#totp-desc "Open your 2FA app, and input the 6-digit code it generates."]
   [f/form {:method "POST"}
    [f/input-group
     {:label            "2FA code from authenticator app"
      :id               "code"
      :type             "text"
      :name             "code"
      :required         "required"
      :aria-describedby "totp-desc"
      :autofocus        "autofocus"
      :error            error}]
    [f/submit {:type "submit" :value "Verify 2FA"}]]])
