(ns co.gaiwan.oak.apis.totp
  (:require
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.html.layout :as layout]
   [co.gaiwan.oak.lib.auth-middleware :as auth-mw]
   [co.gaiwan.oak.lib.form :as form]
   [co.gaiwan.oak.lib.totp :as totp]
   [co.gaiwan.oak.util.routing :as routing]
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identifier :as identifier]
   [lambdaisland.hiccup.middleware :as hiccup-mw]
   [ring.middleware.anti-forgery :as ring-csrf]))

;; 1. generate secret, store in session, show QR code
;; 2. user adds it to their authenticator app
;; 3. ask user for code from authenticator
;; 4. if code is valid, store secret as credential, remove from session

(defn GET-setup
  [{:keys [db session] :as req}]
  (let [secret (totp/secret
                (config/get :totp/hash-alg)
                (config/get :totp/secret-size))
        identifier (identifier/find-one db {:identity-id (:identity session)  :type "email"})
        user-email (:identifier/value identifier)]
    {:status 200
     :session (assoc session :totp/secret secret)
     :html/body
     ;; move hiccup to co.gaiwan.oak.html.*
     [:p "Set up TOTP here!"
      [:img {:src
             (totp/qrcode-data-url
              {:secret secret
               :label user-email
               :issuer (config/get :application/name)})}]
      [:a {:href (routing/url-for req :totp/verify)} "Continue"]]}))

(defn GET-verify [req]
  {:status 200
   :html/body
   [form/form {:method "POST"}
    [:input {:type "text" :name "code"}]
    [:input {:type "submit" :value "Verify 2FA Setup"}]]})

(defn verified-success
  "Store secret as credential, remove the secret from the session"
  [{:keys [db session]} secret]
  (let [opts {:identity-id (:identity session)
              :type "totp"
              :value secret}
        updated-session (dissoc session :totp/secret)]
    (if (credential/create! db opts)

      {:status 200
       :session updated-session
       :html/body
       [:div "Your authenticator device has been successfully linked."]}
      {:status 200
       :html/body
       [:div "Encountering error when recording credentials"]})))

(defn verified-failed [req]
  (tap> req)
  {:status 200
   :html/body
   [:div "Invalid code. Please check and re-enter."
    [:a {:href (routing/url-for req :totp/verify)} "Continue"]]})

(defn POST-verify
  {:parameters {:form [:map
                       [:code string?]]}}
  [req]
  (let [code (-> req :parameters :form :code)
        secret (-> req :session :totp/secret)]
    ;; if valid, store as credential with type="totp"
    (if (totp/verify-code secret code)
      (verified-success req secret)
      (verified-failed req))))

(defn component [opts]
  {:routes
   ["" {}
    ["/totp" {:html/layout layout/layout
              :middleware  [ring-csrf/wrap-anti-forgery
                            auth-mw/wrap-session-auth
                            hiccup-mw/wrap-render]}
     ["/setup" {:name        :totp/setup
                :get         #'GET-setup}]
     ["/verify" {:name :totp/verify
                 :get #'GET-verify
                 :post #'POST-verify}]]]})
