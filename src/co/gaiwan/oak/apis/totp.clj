(ns co.gaiwan.oak.apis.totp
  "2FA endpoints.
      - generate secret, store in session, show QR code
      - user adds it to their authenticator app
      - ask user for code from authenticator
      - if code is valid, store secret as credential, remove from session"
  (:require
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.html.layout :as layout]
   [co.gaiwan.oak.lib.auth-middleware :as auth-mw]
   [co.gaiwan.oak.lib.form :as form]
   [co.gaiwan.oak.lib.totp :as totp]
   [co.gaiwan.oak.html.totp :as html]
   [co.gaiwan.oak.util.routing :as routing]
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identifier :as identifier]
   [lambdaisland.hiccup.middleware :as hiccup-mw]
   [ring.middleware.anti-forgery :as ring-csrf]))

(defn GET-setup
  [{:keys [identity db session] :as req}]
  (let [secret (totp/secret
                (config/get :totp/hash-alg)
                (config/get :totp/secret-size))
        id (:identity/id identity)
        identifier (identifier/find-one db {:identity-id id :type "email"})
        user-email (:identifier/value identifier)]
    {:status 200
     :session (assoc session :totp/secret secret)
     :html/body
     (html/setup-page {:data-uri (totp/qrcode-data-url
                                  {:secret secret
                                   :label user-email
                                   :issuer (config/get :application/name)})
                       :next-uri (routing/url-for req :totp/verify)})}))

(defn GET-verify [req]
  {:status 200
   :html/body
   [form/form {:method "POST"}
    [:input {:type "text" :name "code"}]
    [:input {:type "submit" :value "Verify 2FA Setup"}]]})

(defn verified-success
  "Tell success, store secret as credential (upsert the record), remove the secret from
   the session"
  [{:keys [identity db session]} secret]
  (let [opts {:identity-id (:identity/id identity)
              :type "totp"
              :value secret}
        updated-session (dissoc session :totp/secret)]
    (if (credential/create-or-update! db opts)
      {:status 200
       :session updated-session
       :html/body
       (html/verify-success-page {:cred-save-success? true})}
      {:status 200
       :html/body
       (html/verify-success-page {:cred-save-success? false})})))

(defn verified-failed [req]
  {:status 200
   :html/body
   (html/verify-failed-page {:next-uri (routing/url-for req :totp/verify)})})

(defn POST-verify
  {:parameters {:form [:map
                       [:code string?]]}}
  [{:keys [identity db session parameters] :as req}]
  (let [code (-> parameters :form :code)
        secret (-> session :totp/secret)]
    (if (totp/verify-code secret code)
      (verified-success req secret)
      (verified-failed req))))

(defn component [opts]
  {:routes
   ["" {}
    ["/totp" {:html/layout layout/layout
              :middleware  [ring-csrf/wrap-anti-forgery
                            auth-mw/wrap-session-auth
                            auth-mw/wrap-enforce-login
                            hiccup-mw/wrap-render]}
     ["/setup" {:name        :totp/setup
                :get         #'GET-setup}]
     ["/verify" {:name :totp/verify
                 :get #'GET-verify
                 :post #'POST-verify}]]]})
