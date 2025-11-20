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

(defn POST-password
  {:parameters
   {:form
    [:map
     [:current-password [:string {:min 1}]]
     [:new-password [:string {:min (config/get :password/min-length)}]]]}}
  [{:keys [db session parameters] :as req}]
  (let [current-pass (-> parameters :form :current-password)
        new-pass (-> parameters :form :new-password)
        identity-id (get-in req [:identity :identity/id])
        valid-current? (cred/verify-password db {:identity-id identity-id
                                                 :password current-pass})]
    (cond
      (not valid-current?)
      {:status 303
       :flash {:error "Current password is incorrect"}
       :headers {"Location" (routing/url-for req :home/dash)}}
      (= current-pass new-pass)
      {:status 303
       :flash {:error "New password must be different from current password"}
       :headers {"Location" (routing/url-for req :home/dash)}}
      :else
      (do
        (cred/set-password! db {:identity-id identity-id :password new-pass})
        {:status 303
         :flash {:success "Password updated successfully"}
         :headers {"Location" (routing/url-for req :home/dash)}}))))

(defn POST-auth-apps
  {:parameters
   {:form
    [:map
     [:auth-id uuid?]]}}
  [{:keys [db session parameters] :as req}]
  (let [auth-id (-> parameters :form :auth-id)
        identity-id (get-in req [:identity :identity/id])]
    (oauth-authorization/remove-auth! db {:id auth-id :identity-id identity-id})
    {:status 303
     :headers {"Location" (routing/url-for req :home/dash)}}))

(defn POST-2fa
  {:parameters
   {:form
    [:map
     [:enable-2fa boolean?]]}}
  [{:keys [db session parameters] :as req}]
  (let [enable-2fa (-> parameters :form :enable-2fa)
        identity-id (get-in req [:identity :identity/id])]
    (if enable-2fa
      {:status 303
       :headers {"Location" (routing/url-for req :totp/setup)}}
      (do (cred/disable-totp! db {:identity-id identity-id})
          {:status 303
           :headers {"Location" (routing/url-for req :home/dash)}}))))

(defn GET-dashboard [{:keys [db] :as req}]
  {:status 200
   :html/body
   [dash-html/dash-page
    {:req req
     :debug? (get-in req [:params :debug])
     :enable-2fa? (some? (cred/exist-totp? db {:identity-id (get-in req [:identity :identity/id])}))
     :authorized-apps (oauth-authorization/get-apps db {:identity-id (get-in req [:identity :identity/id])})
     :totp-setup-url (routing/url-for req :totp/setup)
     :logout-url (routing/url-for req :auth/logout)}]})

(defn component [opts]
  {:routes
   ["" {}
    ["/" {:name :home/dash
          :html/layout layout/layout
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
    ["/dashboard/auth-apps" {:name :home/dash-auth-apps
                             :middleware [auth-mw/wrap-session-auth
                                          auth-mw/wrap-enforce-login
                                          hiccup-mw/wrap-render]
                             :post #'POST-auth-apps}]
    ["/dashboard/2fa" {:name :home/dash-2fa
                       :middleware [auth-mw/wrap-session-auth
                                    auth-mw/wrap-enforce-login
                                    hiccup-mw/wrap-render]
                       :post #'POST-2fa}]]})
