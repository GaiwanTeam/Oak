(ns co.gaiwan.oak.apis.auth
  "Authentication endpoints. Login, logout, etc."
  (:require
   [clojure.java.io :as io]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.html.layout :as layout]
   [co.gaiwan.oak.html.login-page :as login-form]
   [co.gaiwan.oak.lib.auth-middleware :as auth-mw]
   [co.gaiwan.oak.util.routing :as routing]
   [lambdaisland.hiccup.middleware :as hiccup-mw]
   [ring.middleware.anti-forgery :as ring-csrf]))

(defn GET-login [req]
  {:status 200
   :html/body [login-form/login-html]
   :html/head [:title "Oak Login"]})

(defn POST-login
  {:parameters
   {:form
    {:email string?
     :password string?}}}
  [{:keys [db parameters session] :as req}]
  (if-let [id (identity/validate-login db (:form parameters))]
    (if-let [url (:redirect-after-login session)]
      {:status 302
       :headers {"Location" url}
       :session {:identity id
                 :auth-time (System/currentTimeMillis)}}
      {:status 200
       :html/body [:p "Successfully authenticated"]
       :session {:identity id
                 :auth-time (System/currentTimeMillis)}})
    {:status 403
     :html/body [:p "Invalid credentials"]}))

(defn GET-logout [req]
  {:status 302
   :headers {"Location" (routing/path-for req :auth/login)}
   :session ^:replace {}})

(defn component [opts]
  {:routes
   ["/auth" {}
    ["/login" {:name :auth/login
               :html/layout layout/layout
               :middleware [ring-csrf/wrap-anti-forgery
                            auth-mw/wrap-session-auth
                            hiccup-mw/wrap-render]
               :get #'GET-login
               :post #'POST-login}]
    ["/logout" {:name :auth/logout
                :get #'GET-logout}]]})
