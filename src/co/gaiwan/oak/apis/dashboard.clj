(ns co.gaiwan.oak.apis.dashboard
  "Front page, some basic account management for the user"
  (:require
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

(defn POST-dashboard
  {:parameters
   {:form
    [:map
     [:new-password string?]]}}
  [{:keys [db session parameters] :as req}]
  (let [new-pass (-> parameters :form :new-password)
        identity-id (get-in req [:identity :identity/id])]
    (prn :update {:identity-id identity-id :password new-pass})
    (cred/set-password! db {:identity-id identity-id :password new-pass})
    {:status 200
     :html/body [:div
                 [:p "success"]]}))

(defn POST-auth-apps
  {:parameters
   {:form
    [:map
     [:auth-id uuid?]]}}
  [{:keys [db session parameters] :as req}]
  (let [auth-id (-> parameters :form :auth-id)
        identity-id (get-in req [:identity :identity/id])]
    (oauth-authorization/remove-auth! db {:id auth-id})
    {:status 200
     :html/body [:div
                 [:p "success"]]}))

(defn GET-dashboard [req]
  {:status 200
   :html/body (dash-html/dash-page
               {:req req
                :debug? (get-in req [:params :debug])
                :authorized-apps (oauth-authorization/get-apps (:db req))
                :totp-setup-url (routing/url-for req :totp/setup)
                :logout-url (routing/url-for req :auth/logout)})})

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
          :post #'POST-dashboard
          :get #'GET-dashboard}]
    ["/dashboard/auth-apps" {:name :home/dash-update
                             :middleware [wrap-params
                                          wrap-keyword-params
                                          ring-csrf/wrap-anti-forgery
                                          auth-mw/wrap-session-auth
                                          auth-mw/wrap-enforce-login
                                          hiccup-mw/wrap-render]
                             :post #'POST-auth-apps}]]})
