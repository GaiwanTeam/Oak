(ns co.gaiwan.oak.apis.dashboard
  "Front page, some basic account management for the user"
  (:require
   [co.gaiwan.oak.html.layout :as layout]
   [co.gaiwan.oak.lib.auth-middleware :as auth-mw]
   [lambdaisland.hiccup.middleware :as hiccup-mw]
   [ring.middleware.anti-forgery :as ring-csrf]))

(defn GET-dashboard [req]
  )

(defn component [opts]
  {:routes
   ["/" {:name :home/dash
         :html/layout layout/layout
         :middleware [ring-csrf/wrap-anti-forgery
                      auth-mw/wrap-session-auth
                      auth-mw/wrap-enforce-login
                      hiccup-mw/wrap-render]
         :get #'GET-dashboard
         }]})
