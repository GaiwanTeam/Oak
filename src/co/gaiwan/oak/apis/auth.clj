(ns co.gaiwan.oak.apis.auth
  "Authentication endpoints. Login, logout, etc."
  (:require
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.lib.form :as form]
   [lambdaisland.hiccup.middleware :as hiccup-mw]
   [ring.middleware.anti-forgery :as ring-csrf]))

(defn login-html []
  [form/form {:method "POST"}
   [:label {:for "email"} "Email"
    [:input {:id "email" :name "email" :type "text"}]]
   [:label {:for "password"} "Password"
    [:input {:id "password" :name "password" :type "password"}]]
   [:input {:type "submit"}]])

(defn GET-login [req]
  {:status 200
   :html/body [login-html]
   :html/head [:title "Oak Login"]})

(defn POST-login
  {:parameters
   {:form
    {:email string?
     :password string?}}
   :middleware [ring-csrf/wrap-anti-forgery]}
  [{:keys [db parameters session] :as req}]
  (if-let [id (identity/validate-login db (:form parameters))]
    (if-let [url (:redirect-after-login session)]
      {:status 302
       :headers {"Location" url}
       :session {:identity id}}
      {:status 200
       :html/body [:p "Successfully authenticated"]
       :session {:identity id}})
    {:status 403
     :html/body [:p "Invalid credentials"]}))

(defn component [opts]
  {:routes
   ["/auth" {}
    ["/login" {:name :auth/login
               :middleware [hiccup-mw/wrap-render]
               :get #'GET-login
               :post #'POST-login}]]})
