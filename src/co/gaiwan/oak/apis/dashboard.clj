(ns co.gaiwan.oak.apis.dashboard
  "Front page, some basic account management for the user"
  (:require
   [co.gaiwan.oak.html.layout :as layout]
   [co.gaiwan.oak.lib.auth-middleware :as auth-mw]
   [co.gaiwan.oak.html.dashboard :as dash-html]
   [co.gaiwan.oak.util.routing :as routing]
   [lambdaisland.hiccup.middleware :as hiccup-mw]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.anti-forgery :as ring-csrf]))

(defn GET-dashboard [req]
  {:status 200
   :html/body (dash-html/dash-page
               {:req req
                :debug? (get-in req [:params :debug])
                :totp-setup-url (routing/url-for req :totp/setup)
                :logout-url (routing/url-for req :auth/logout)})})

(defn component [opts]
  {:routes
   ["/" {:name :home/dash
         :html/layout layout/layout
         :middleware [wrap-params
                      wrap-keyword-params
                      ring-csrf/wrap-anti-forgery
                      auth-mw/wrap-session-auth
                      auth-mw/wrap-enforce-login
                      hiccup-mw/wrap-render]
         :get #'GET-dashboard}]})
