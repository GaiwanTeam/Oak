(ns co.gaiwan.oak.apis.auth
  "Authentication endpoints. Login, logout, etc."
  (:require
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identifier :as identifier]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.html.email.password-reset :as email-views]
   [co.gaiwan.oak.html.layout :as layout]
   [co.gaiwan.oak.html.login-page :as login-form]
   [co.gaiwan.oak.html.auth :as views]
   [co.gaiwan.oak.lib.auth-middleware :as auth-mw]
   [co.gaiwan.oak.lib.db :as db]
   [co.gaiwan.oak.lib.email :as email]
   [co.gaiwan.oak.util.random :as random]
   [co.gaiwan.oak.util.routing :as routing]
   [lambdaisland.hiccup.middleware :as hiccup-mw]
   [ring.middleware.anti-forgery :as ring-csrf])
  (:import
   (java.time Instant)
   (java.time.temporal ChronoUnit)))

(defn GET-login
  {:parameters
   {:query [:map [:id {:optional true} string?]]}}
  [req]
  (if (:identity req)
    {:status 302
     :headers {"Location" (routing/url-for req :home/dash)}}
    {:status 200
     :html/body [login-form/login-html req {:identifier (-> req :parameters :query :id)}]
     :html/head [:title "Oak Login"]}))

(defn POST-login
  {:parameters
   {:form
    {:identifier string?
     :password string?}}}
  [{:keys [db parameters session] :as req}]
  (let [identity-id (identity/validate-login db (:form parameters))
        totp-cred (when identity-id (credential/find-one db {:identity-id identity-id :type credential/type-totp}))
        session (if identity-id
                  (with-meta
                    (-> session
                        (assoc :identity identity-id)
                        (update-in [:authentications identity-id]
                                   (fnil conj #{})
                                   {:type credential/type-password
                                    :created-at (Instant/now)}))
                    {:recreate true})
                  session)]
    (cond
      ;; login failed, show form again, don't touch session
      (not identity-id)
      {:status 403
       :html/body [login-form/login-html req {:identifier (:identifier (:form parameters))
                                              :error "Invalid username or password"}]}

      ;; login passed, but we still need to do totp
      totp-cred
      {:status 302
       :headers {"Location" (routing/path-for req :totp/check)}
       :session session}

      ;; login passed, and we have a place to send the user (probably OAuth2 redirect)
      (:redirect-after-login session)
      {:status 302
       :headers {"Location" (:redirect-after-login session)}
       :session (dissoc session :redirect-after-login)}

      ;; login passed, show success page
      :else
      {:status 200
       :html/body [views/success-page req {:title "Successfully authenticated"}]
       :session session})))

(defn GET-logout [req]
  {:status 302
   :headers {"Location" (routing/path-for req :auth/login)}
   :session ^:replace {}})

(defn GET-request-password-reset [req]
  {:status 200
   :html/body [views/password-reset-html req]
   :html/head [:title "Reset Password"]})

(defn POST-request-password-reset
  {:parameters
   {:form
    {:email string?}}}
  [{:keys [db parameters] :as req}]
  (let [email (-> parameters :form :email)
        expiry-hours (config/get :password-reset/link-expiry-hours)
        org-name (config/get :org/name)]
    (future
      (when-let [{:identifier/keys [identity-id]} (identifier/find-one db {:type "email" :value email :primary true})]
        (let [nonce (random/secure-base62-str 400)]
          (credential/create! db {:identity-id identity-id
                                  :type credential/type-password-reset-nonce
                                  :value nonce
                                  :expires-at (.plus (Instant/now) expiry-hours ChronoUnit/HOURS)})
          (email/send! {:to email
                        :subject (str "Your " org-name " password reset link")
                        :html (email-views/reset-html
                               {:name email
                                :org-name org-name
                                :reset-link (routing/url-for req :auth/submit-password-reset {:nonce nonce})
                                :expiry-hours expiry-hours})}))))
    {:status 200
     :html/body [views/password-reset-requested req {:email email
                                                     :expiry-hours expiry-hours}]
     :html/head [:title "Reset Password"]}))

(defn- resolve-nonce [db nonce]
  (when-let [{:credential/keys [id identity-id]}
             (credential/find-one db {:type  credential/type-password-reset-nonce
                                      :value nonce})]
    (let [{:identifier/keys [value]} (identifier/find-one db {:type "email" :identity-id identity-id :primary true})]
      {:identity-id identity-id
       :nonce-id id
       :email value})))

(defn GET-submit-password-reset
  {:parameters
   {:path {:nonce string?}}}
  [{:keys [parameters db] :as req}]
  (if-let [{:keys [email]} (resolve-nonce db (-> parameters :path :nonce))]
    {:status 200
     :html/body [views/password-reset-form req {:email email
                                                :minlength (config/get :password/min-length)}]}
    {:status 400
     :html/body [views/error-page req "Invalid or expired link"]}))

(defn POST-submit-password-reset
  {:parameters
   {:path {:nonce string?}
    :form {:password string?
           :confirm-password string?}}}
  [{:keys [parameters db] :as req}]
  (if-let [{:keys [nonce-id identity-id email]} (resolve-nonce db (-> parameters :path :nonce))]
    (let [{:keys [password confirm-password]} (:form parameters)
          min-len (config/get :password/min-length)]
      (cond
        (not= password confirm-password)
        {:status 400
         :html/body
         [views/password-reset-form req
          {:email email
           :minlength min-len
           :password password
           :confirm-password confirm-password
           :confirm-error "Passwords do not match"}]}

        (< (count password) min-len)
        {:status 400
         :html/body
         [views/password-reset-form req
          {:email email
           :minlength min-len
           :password password
           :confirm-password confirm-password
           :password-error (str "Passwords should be at least " min-len " characters")}]}

        :else
        (do
          (db/with-transaction [conn db]
            (credential/delete! conn {:id nonce-id})
            (credential/set-password! conn {:identity-id identity-id :password password}))
          {:status 200
           :html/body [views/password-reset-success req]})))
    {:status 400
     :html/body [views/error-page req "Invalid or expired link"]}))

(defn component [opts]
  {:routes
   ["/auth" {}
    ["/login"
     {:name :auth/login
      :html/layout layout/layout
      :middleware [ring-csrf/wrap-anti-forgery
                   auth-mw/wrap-session-auth
                   hiccup-mw/wrap-render]
      :get #'GET-login
      :post #'POST-login}]
    ["/logout"
     {:name :auth/logout
      :get #'GET-logout}]
    ["/reset-password" {:html/layout layout/layout
                        :middleware [ring-csrf/wrap-anti-forgery
                                     hiccup-mw/wrap-render]}
     [""
      {:name :auth/password-reset
       :get #'GET-request-password-reset
       :post #'POST-request-password-reset}]
     ["/:nonce"
      {:name :auth/submit-password-reset
       :get #'GET-submit-password-reset
       :post #'POST-submit-password-reset}]]]})
