(ns co.gaiwan.oak.lib.ring-csp
  "Middleware that sets a CSP policy"
  (:require
   [co.gaiwan.oak.app.config :as config]))

(defn wrap-content-security-policy
  "Add the Content-Security-Policy header, see configuration."
  [h]
  (fn [req]
    (let [res (h req)]
      (assoc-in res
                {:headers "content-security-policy"}
                (config/get :http/csp-policy)))))
