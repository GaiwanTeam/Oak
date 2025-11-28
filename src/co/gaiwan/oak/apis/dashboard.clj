(ns co.gaiwan.oak.apis.dashboard
  "Front page, some basic account management for the user"
  (:require
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.html.layout :as layout]
   [co.gaiwan.oak.lib.auth-middleware :as auth-mw]
   [co.gaiwan.oak.html.dashboard :as dash-html]
   [co.gaiwan.oak.util.routing :as routing]
   [co.gaiwan.oak.domain.credential :as cred]
   [co.gaiwan.oak.domain.oauth-authorization :as oauth-authorization]
   [lambdaisland.hiccup.middleware :as hiccup-mw]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.anti-forgery :as ring-csrf]))

(defn- get-primary-identifiers [db identity-id]
  (-> (group-by :identifier/type
                (co.gaiwan.oak.domain.identifier/find-all
                 db
                 {:identity-id identity-id
                  :primary     true}))
      (update-vals #(map :identifier/value %))
      (update-keys keyword)))

(defn- dashboard-response [{:keys [db] :as req} {:keys [password-error new-password-error confirm-error]
                                                 :as extra-args}]
  (let [identity-id (get-in req [:identity :identity/id])]
    {:status (if (or password-error new-password-error confirm-error)
               400
               200)
     :html/body
     [dash-html/dash-page
      (merge
       {:req req
        :identifiers (get-primary-identifiers db identity-id)
        :debug? (some? (get-in req [:params :debug]))
        :enable-2fa? (some? (cred/exist-totp? db {:identity-id identity-id}))
        :authorized-apps (oauth-authorization/get-apps db {:identity-id identity-id})
        :totp-setup-url (routing/url-for req :totp/setup)
        :logout-url (routing/url-for req :auth/logout)}
       extra-args)]}))

(defn POST-password
  {:summary "Update user password"
   :description "Allows authenticated users to change their password. Validates the current password and enforces minimum length requirements."
   :tags ["account"]
   :parameters
   {:form
    [:map
     [:current-password [:string {:min 1}]]
     [:new-password [:string]]
     [:confirm-new-password [:string]]]}
   :responses
   {200 {:description "Password updated successfully"}
    400 {:description "Submission not valid"}}}
  [{:keys [db session parameters] :as req}]
  (let [current-pass (-> parameters :form :current-password)
        new-pass (-> parameters :form :new-password)
        confirm-pass (-> parameters :form :confirm-new-password)
        identity-id (get-in req [:identity :identity/id])
        valid-current? (cred/verify-password db {:identity-id identity-id
                                                 :password current-pass})]
    (cond
      (not valid-current?)
      (dashboard-response req {:password-error "The current password you entered is incorrect"})

      (< (count new-pass) (config/get :password/min-length))
      (dashboard-response req {:new-password-error
                               (format "The new password should be at least %d characters long"
                                       (config/get :password/min-length))})

      (= current-pass new-pass)
      (dashboard-response req {:new-password-error "The new password you chose is identical to old password"})

      (not= new-pass confirm-pass)
      (dashboard-response req {:confirm-password-error "The new password and confirmation password do not match"})

      :else
      (do
        (cred/set-password! db {:identity-id identity-id :password new-pass})
        {:status 303
         :flash {:success "Your password updated successfully"}
         :headers {"Location" (routing/url-for req :home/dash)}}))))

(defn DELETE-auth-app
  {:summary "Revoke application authorization"
   :description "Removes OAuth authorization for a specific third-party application. The authenticated user can only revoke their own authorizations."
   :tags ["oauth" "security"]
   :parameters
   {:form
    [:map
     [:auth-id uuid?]]}
   :responses
   {303 {:description "Authorization revoked successfully"
         :headers {"Location" {:schema :string}}}}}
  [{:keys [db session parameters] :as req}]
  (let [auth-id (-> parameters :form :auth-id)
        identity-id (get-in req [:identity :identity/id])]
    (oauth-authorization/remove-auth! db {:id auth-id :identity-id identity-id})
    {:status 303
     :headers {"Location" (routing/url-for req :home/dash)}}))

(defn POST-2fa
  {:summary "Configure two-factor authentication"
   :description "Enable or disable two-factor authentication (TOTP) for the authenticated user. Enabling 2FA redirects to setup instructions."
   :tags ["security" "account"]
   :parameters
   {:form
    [:map
     [:enable-2fa boolean?]]}
   :responses
   {303 {:description "2FA configuration updated, redirects to setup or dashboard"
         :headers {"Location" {:schema :string}}}}}
  [{:keys [db session parameters] :as req}]
  (let [enable-2fa (-> parameters :form :enable-2fa)
        identity-id (get-in req [:identity :identity/id])]
    (if enable-2fa
      {:status 303
       :headers {"Location" (routing/url-for req :totp/setup)}}
      (do (cred/disable-totp! db {:identity-id identity-id})
          {:status 303
           :headers {"Location" (routing/url-for req :home/dash)}}))))

(defn GET-dashboard
  {:summary "Display user dashboard"
   :description "Renders the main dashboard page with account information, 2FA status, password management, and authorized applications for the authenticated user."
   :tags ["account" "ui"]
   :parameters
   {:query
    [:map
     {:optional true}
     [:debug {:optional true} boolean?]]}
   :responses
   {200 {:description "Dashboard page rendered successfully"}}}
  [{:keys [db] :as req}]
  (dashboard-response req {}))

(defn component [opts]
  {:routes
   ["" {:html/layout layout/layout}
    ["/" {:name :home/dash
          :middleware [wrap-params
                       wrap-keyword-params
                       ring-csrf/wrap-anti-forgery
                       auth-mw/wrap-session-auth
                       auth-mw/wrap-enforce-login
                       hiccup-mw/wrap-render]
          :get #'GET-dashboard}]
    ["/dashboard/password" {:name :home/dash-password
                            :middleware [auth-mw/wrap-session-auth
                                         auth-mw/wrap-enforce-login
                                         hiccup-mw/wrap-render]
                            :post #'POST-password}]
    ["/dashboard/2fa" {:name :home/dash-2fa
                       :middleware [auth-mw/wrap-session-auth
                                    auth-mw/wrap-enforce-login
                                    hiccup-mw/wrap-render]
                       :post #'POST-2fa}]
    ["/authorizations/app" {:name :authorizations/app
                            :middleware [auth-mw/wrap-session-auth
                                         auth-mw/wrap-enforce-login
                                         hiccup-mw/wrap-render]
                            :delete #'DELETE-auth-app}]]})
