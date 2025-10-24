(ns co.gaiwan.oak.html.totp
  (:require
   [co.gaiwan.oak.html.forms :as f]
   [co.gaiwan.oak.html.tokens :refer :all]
   [co.gaiwan.oak.html.widgets :as w]
   [co.gaiwan.oak.html.forms :as form]
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

(defn fail-page [{:keys [next-uri]}]
  [totp-layout
   [:h1 "2FA Setup Failed"]
   [:p "Invalid code. Please check and re-enter."]
   [:a {:href next-uri} "Go back to 2FA setup page"]])

(defn success-page [{:keys [cred-save-success?]}]
  [totp-layout
   (if cred-save-success?
     [:<>
      [:h1 "2FA Enabled"]
      [:div "Your authenticator app has been successfully linked."]]
     [:<>
      [:h1 "2FA Setup Failed"]
      [:div "Encountering error when recording credentials"]])])

(o/defstyled qr-img :img
  {:max-width "80%"
   :margin "0 auto"})

(defn setup-page
  ([{:keys [data-uri next-uri]}]
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
       :autofocus        true}]
     [f/submit {:type "submit" :value "Enable 2FA"}]]]))
