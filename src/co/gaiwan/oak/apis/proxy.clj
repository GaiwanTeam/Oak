(ns co.gaiwan.oak.apis.proxy
  "reverse proxy to load resources from 3rd website"
  (:require
   [ring.util.response :as response]
   [co.gaiwan.oak.lib.auth-middleware :as auth-mw]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]))

(defn proxy-image-handler
  "Verify this feature:
   using path parameter
   http get to URI: http://localhost:4800/proxy/image/${oauth-client-id}"
  [{:keys [db path-params]}]
  (let [client-id (get path-params :client-id)
        oauth-client (oauth-client/find-by-client-id db client-id)
        byte-image (:oauth-client/logo-image oauth-client)]
    (if byte-image
      (-> (response/response byte-image)
          (response/content-type "image/png")
          (response/header "Cache-Control" "max-age=31536000, public"))
      (response/not-found "Unknown logo key."))))

(defn component [_]
  {:routes
   ["" {}
    ["/proxy/image/:client-id" {:name :proxy/image
                                :middleware [auth-mw/wrap-session-auth
                                             auth-mw/wrap-enforce-login]
                                :get #'proxy-image-handler}]]})
