(ns co.gaiwan.oak.apis.totp
  (:require
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.html.layout :as layout]
   [co.gaiwan.oak.lib.auth-middleware :as auth-mw]
   [co.gaiwan.oak.lib.form :as form]
   [co.gaiwan.oak.lib.totp :as totp]
   [co.gaiwan.oak.util.routing :as routing]
   [lambdaisland.hiccup.middleware :as hiccup-mw]
   [ring.middleware.anti-forgery :as ring-csrf]))

;; 1. generate secret, store in session, show QR code
;; 2. user adds it to their authenticator app
;; 3. ask user for code from authenticator
;; 4. if code is valid, store secret as credential, remove from session

(defn GET-setup
  [req]
  (let [secret (totp/secret
                (config/get :totp/hash-alg)
                (config/get :totp/secret-size))]
    {:status 200
     :session (assoc (:session req) :totp/secret secret)
     :html/body
     ;; move hiccup to co.gaiwan.oak.html.*
     [:p "Set up TOTP here!"
      [:img {:src
             (totp/qrcode-data-url
              {:secret secret
               ;; You can get user primary email identifier
               :label "..."
               :issuer (config/get :application/name)})
             }]
      [:a {:href (routing/url-for req :totp/verify)} "Continue"]]}))

(defn GET-verify [req]
  {:status 200
   :html/body
   [form/form {:method "POST"}
    [:input {:type "text" :name "code"}]
    [:input {:type "submit" :value "Verify 2FA Setup"}]
    ]}  )

(defn POST-verify
  {:parameters {:form [:map
                       [:code string?]]}}
  [req]
  (let [secret (-> req :session :totp/secret)]
    ;; if valid, store as credential with type="totp"
    )
  )

(defn component [opts]
  {:routes
   ["" {}
    ["/totp" {:html/layout layout/layout
              :middleware  [ring-csrf/wrap-anti-forgery
                            auth-mw/wrap-session-auth
                            hiccup-mw/wrap-render]}
     ["/setup" {:name        :totp/setup
                :get         #'GET-setup}]
     ["/verify" {:name :totp/verify
                 :get #'GET-verify
                 :post #'POST-verify}]]]})
