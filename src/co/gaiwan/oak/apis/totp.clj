(ns co.gaiwan.oak.apis.totp
  "2FA endpoints.
      - generate secret, store in session, show QR code
      - user adds it to their authenticator app
      - ask user for code from authenticator
      - if code is valid, store secret as credential, remove from session"
  (:require
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.domain.backup-code :as backup-code]
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identifier :as identifier]
   [co.gaiwan.oak.domain.backup-code :as backup-code]
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
  {:summary "Display TOTP setup page"
   :description "Generate a new TOTP secret, display its QR code, and prepare the user to add it to their authenticator app"
   :tags ["2FA" "TOTP"]
   :responses {200 {:description "Setup page with QR code displayed"}}}
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

(defn POST-setup
  {:summary "Verify and enable TOTP"
   :description "Verify the TOTP code provided by user, store the secret as a credential if valid, and remove it from session"
   :tags ["2FA" "TOTP"]
   :parameters
   {:form
    [:map
     [:code string?]]}
   :responses {200 {:description "TOTP successfully enabled"}
               400 {:description "Invalid or missing TOTP code"}}}
  [{:keys [db session parameters] :as req}]
  (let [code   (-> parameters :form :code)
        secret (-> session :totp/secret)
        identity-id (:identity session)]
    (if (and code secret (totp/verify-code secret code))
      (do
        (credential/enable-totp! db {:identity-id identity-id :secret secret})
        (let [codes (backup-code/generate! db {:identity-id identity-id})]
          {:status  200
           :session (dissoc session :totp/secret)
           :html/body
           [totp-html/success-page req codes]}))
      (let [identifier  (identifier/find-one db {:identity-id identity-id :type "email" :primary true})
            user-email  (:identifier/value identifier)]
        {:status 400
         :html/body
         [totp-html/setup-page
          {:data-uri (totp/qrcode-data-url
                      {:secret (:totp/secret session)
                       :label  user-email
                       :issuer (config/get :application/name)})}]}))))

(defn GET-check
  {:summary "Display TOTP verification page"
   :description "Show the form for user to enter their TOTP code during login"
   :tags ["2FA" "TOTP"]
   :responses {200 {:description "2FA verification form displayed"}}}
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
  {:summary "Verify TOTP code during login"
   :description "Verify the TOTP code submitted by user during login authentication, update session if valid, and redirect accordingly"
   :tags ["2FA" "TOTP"]
   :parameters
   {:form [:map
           [:code string?]]}
   :responses {200 {:description "Authentication successful, TOTP verified"}
               302 {:description "Authentication successful, redirecting to originally requested page"}
               400 {:description "Invalid, expired, or missing TOTP code"}}}
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
                      (auth-mw/update-session-auth!
                       session {:type       credential/type-totp
                                :created-at (Instant/now)}))]
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

(defn GET-backup-code
  {:summary "Display backup-code verification page"
   :description "Show the form for user to enter their backup code during login"
   :tags ["2FA" "backup-code"]
   :responses {200 {:description "2FA backup code verification form displayed"}}}
  [{:keys [db session] :as req}]
  {:status 200
   :html/body [totp-html/backup-code-page {}]})

(defn POST-backup-code
  {:summary "Verify backup code during login"
   :description "Verify the backup code submitted by user during login authentication, update session if valid, and redirect accordingly"
   :tags ["2FA" "backup-code"]
   :parameters
   {:form [:map
           [:code string?]]}
   :responses {200 {:description "Authentication successful, backup code verified"}
               302 {:description "Authentication successful, redirecting to originally requested page"}
               400 {:description "Invalid, expired, or missing TOTP code"}}}
  [{:keys [db session parameters] :as req}]
  (let [code        (-> parameters :form :code)
        identity-id (:identity session)]
    (if-not (and code identity-id)
      {:status    400
       :html/body [auth-html/error-page req "backup code failed: bad request or session. Please try logging in again."]}
      (let [id (backup-code/verify-code db {:identity-id identity-id :code code})
            valid? id
            session (if (not valid?)
                      session
                      (auth-mw/update-session-auth!
                       session {:type       credential/type-totp
                                :created-at (Instant/now)}))]
        (when valid?
          (backup-code/update! db {:id id
                                   :used true}))
        (cond
          (not valid?)
          {:status 400
           :html/body [totp-html/backup-code-page {:error "Invalid or expired code"}]}

          (:redirect-after-login session)
          {:status  302
           :headers {"Location" (:redirect-after-login session)}
           :session (dissoc session :redirect-after-login)}

          :else
          {:status    200
           :html/body [auth-html/success-page req
                       {:title "Successfully authenticated"}
                       [:p "You passed two-factor authentication using backup code!"]
                       (let [unused-code (backup-code/valid-count db {:identity-id identity-id})]
                         (when (< unused-code 5)
                           [:p "You have only " unused-code " backup code left."]))]

           :session   session})))))

(defn component [opts]
  {:routes
   ["" {}
    ["/2fa" {:html/layout layout/layout}
     ["/setup" {:name :totp/setup
                :middleware  [auth-mw/wrap-session-auth
                              auth-mw/wrap-enforce-login
                              ring-csrf/wrap-anti-forgery
                              hiccup-mw/wrap-render]
                :get  #'GET-setup
                :post #'POST-setup}]]
    ["/2fa/check" {:html/layout layout/layout
                   :middleware  [auth-mw/wrap-2fa-in-progress
                                 ring-csrf/wrap-anti-forgery
                                 hiccup-mw/wrap-render]
                   :name        :totp/check
                   :get         #'GET-check
                   :post        #'POST-check}]
    ["/backup-code" {:html/layout layout/layout
                     :middleware  [auth-mw/wrap-2fa-in-progress
                                   ring-csrf/wrap-anti-forgery
                                   hiccup-mw/wrap-render]
                     :name        :backup-code/check
                     :get         #'GET-backup-code
                     :post        #'POST-backup-code}]]})
