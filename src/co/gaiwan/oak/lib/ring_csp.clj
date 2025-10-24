(ns co.gaiwan.oak.lib.ring-csp
  "Middleware that sets a CSP policy.

  CSP policy is configured through `:http/csp-policy`. The special value `nonce`
  is replaced with `nonce-<per-request-generated-nonce>`, which can be accessed
  through `*csp-nonce*`"
  (:require
   [clojure.string :as str]
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.util.random :as random]))

(def ^:dynamic *csp-nonce* nil)

(defn wrap-content-security-policy
  "Add the Content-Security-Policy header, see configuration."
  [h]
  (fn [req]
    (binding [*csp-nonce* (random/secure-base62-str 200)]
      (let  [res (h req)]
        (assoc-in res
                  [:headers "content-security-policy"]
                  (str/replace
                   (config/get :http/csp-policy)
                   #"'nonce'" (str "'nonce-" *csp-nonce* "'")))))))
