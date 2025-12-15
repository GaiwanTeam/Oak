(ns co.gaiwan.oak.html.totp
  (:require
   [charred.api :as charred]
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

(defn success-page [req codes]
  [totp-layout
   [:<>
    [:h1 "2FA Enabled"]
    [:div "Your authenticator app has been successfully linked."]
    [:h3 "Backup Codes"]
    [:div.alert-warning
     "Please save these backup codes into save place. You can use these codes to login when you lost your authenticator device."]
    ;; show the backup codes
    [:pre
     (for [code codes]
       (str code "\n"))]
    ;; Button to download the backup codes
    [:button#download-backup-code.btn-primary
     {:oak-download (charred/write-json-str codes)
      :oak-download-filename "2fa-backup-codes.txt"}
     "Download"]
    [:hr]
    [:p [:a.subtle {:href (routing/url-for req :auth/login)} "Back to login"]]]])

(o/defstyled qr-img :img
  {:max-width "80%"
   :margin "0 auto"})

(defn setup-page [{:keys [data-uri]}]
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
    [f/submit {:type "submit" :value "Verify 2FA"}]]
   [:div
    [:p "Having trouble? "
     [:a {:href "/backup-code"}
      "Use a backup code"]]]])

(defn backup-code-page [{:keys [error]}]
  [totp-layout
   [:h1 "Verify Backup Code"]
   [:p#totp-desc "Enter one of your backup codes to continue"]
   [f/form {:method "POST"}
    [f/input-group
     {:label            "backup code"
      :id               "code"
      :type             "text"
      :name             "code"
      :required         "required"
      :aria-describedby "totp-desc"
      :autofocus        "autofocus"
      :error            error}]
    [f/submit {:type "submit" :value "Verify backup code"}]]])
