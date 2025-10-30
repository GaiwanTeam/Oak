(ns co.gaiwan.oak.apis.totp
  "2FA endpoints.
      - generate secret, store in session, show QR code
      - user adds it to their authenticator app
      - ask user for code from authenticator
      - if code is valid, store secret as credential, remove from session"
  (:require
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identifier :as identifier]
   [co.gaiwan.oak.html.auth :as auth-html]
   [co.gaiwan.oak.html.layout :as layout]
   [co.gaiwan.oak.html.totp :as totp-html]
   [co.gaiwan.oak.lib.auth-middleware :as auth-mw]
   [co.gaiwan.oak.lib.totp :as totp]
   [co.gaiwan.oak.util.routing :as routing]
   [lambdaisland.hiccup.middleware :as hiccup-mw]
   [ring.middleware.anti-forgery :as ring-csrf])
  (:import
   (java.time Instant)))

(defn GET-setup
  [{:keys [db session] :as req}]
  (let [secret      (totp/secret
                     (config/get :totp/hash-alg)
                     (config/get :totp/secret-size))
        identity-id (:identity session)
        identifier  (identifier/find-one db {:identity-id identity-id :type "email" :primary true})
        user-email  (:identifier/value identifier)]
    {:status  200
     :session (assoc session :totp/secret secret)
     :html/body
     [totp-html/setup-page
      {:data-uri (totp/qrcode-data-url
                  {:secret secret
                   :label  user-email
                   :issuer (config/get :application/name)})}]}))

(defn verification-success
  "Tell success, store secret as credential (upsert the record), remove the secret from
   the session"
  [{:keys [db session] :as req} secret]
  (let [identity-id     (:identity session)
        updated-session (dissoc session :totp/secret)]
    (credential/create-or-update! db {:identity-id identity-id :type "totp" :value secret})
    {:status  200
     :session updated-session
     :html/body
     [totp-html/success-page req]}))

(defn POST-setup
  {:parameters
   {:form
    [:map
     [:code string?]]}}
  [{:keys [db session parameters] :as req}]
  (let [code   (-> parameters :form :code)
        secret (-> session :totp/secret)]
    (if (and code secret (totp/verify-code secret code))
      (verification-success req secret)
      (let [identity-id (:identity session)
            identifier  (identifier/find-one db {:identity-id identity-id :type "email" :primary true})
            user-email  (:identifier/value identifier)]
        {:status 400
         :html/body
         [totp-html/setup-page
          {:data-uri (totp/qrcode-data-url
                      {:secret (:totp/secret session)
                       :label  user-email
                       :issuer (config/get :application/name)})}]}))))

(defn GET-check
  [{:keys [db session] :as req}]
  (let [secret (totp/secret
                (config/get :totp/hash-alg)
                (config/get :totp/secret-size))
        identifier-id (:identity session)
        identifier (identifier/find-one db {:identity-id identifier-id :type "email" :primary true})
        user-email (:identifier/value identifier)]
    {:status 200
     :session (assoc session :totp/secret secret)
     :html/body [totp-html/check-page {}]}))

(defn POST-check
  {:parameters {:form [:map
                       [:code string?]]}}
  [{:keys [db session parameters] :as req}]
  (let [code        (-> parameters :form :code)
        identity-id (:identity session)]
    (if-not (and code identity-id)
      {:status    400
       ;; not a very user friendly message, but this is a rare case, e.g. when
       ;; they manage to load the 2FA form, then in another tab reset their
       ;; session, then come back and submit.
       :html/body [auth-html/error-page req "2FA failed: bad request or session. Please try logging in again."]}
      (let [cred    (credential/find-one db {:identity-id identity-id :type credential/type-totp})
            valid?  (when cred (totp/verify-code (:credential/value cred) code))
            session (if (not valid?)
                      session
                      (-> session
                          (update-in [:authentications identity-id]
                                     (fnil conj #{})
                                     {:type       credential/type-totp
                                      :created-at (Instant/now)})))]
        (cond
          (not valid?)
          {:status 400
           :html/body [totp-html/check-page {:error "Invalid or expired code"}]}

          (:redirect-after-login session)
          {:status  302
           :headers {"Location" (:redirect-after-login session)}
           :session (dissoc session :redirect-after-login)}

          :else
          {:status    200
           :html/body [auth-html/success-page req
                       {:title "Successfully authenticated"}
                       [:p "You passed two-factor authentication!"]]
           :session   session})))))

(defn component [opts]
  {:routes
   ["" {}
    ["/2fa" {:html/layout layout/layout}
     ["/setup" {:name :totp/setup
                :middleware  [ring-csrf/wrap-anti-forgery
                              auth-mw/wrap-session-auth
                              hiccup-mw/wrap-render]
                :get  #'GET-setup
                :post #'POST-setup}]]
    ["/2fa/check" {:html/layout layout/layout
                   :middleware  [ring-csrf/wrap-anti-forgery
                                 hiccup-mw/wrap-render]
                   :name        :totp/check
                   :get         #'GET-check
                   :post        #'POST-check}]]})
