(ns co.gaiwan.oak.apis.proxy
  "reverse proxy to load resources from 3rd website"
  (:require
   [ring.util.response :as response]
   [clojure.java.io :as io]
   [hato.client :as hc]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]))

(defn internal-server-error
  "internal server error"
  ([]
   (internal-server-error "Internal Server Error"))
  ([body]
   {:status 500
    :headers {"Content-Type" "text/plain; charset=utf-8"}
    :body body}))

(defn proxy-image-handler
  "Verify this feature:
   using path parameter
   http get to URI: http://localhost:4800/proxy/image/${oauth-client-id}"
  [{:keys [db path-params] :as req}]
  (let [client-id (get path-params :client-id)
        oauth-client (oauth-client/find-by-client-id db client-id)
        target-url (:oauth-client/logo-uri oauth-client)]
    (if target-url
      (try
        (let [http-response (hc/get target-url {:as :byte-array
                                                :throw-exceptions false
                                                :ignore-unexpected-content-type true})]

          (if (= 200 (:status http-response))
            (let [body (:body http-response)
                  content-type (get-in http-response [:headers "content-type"] "application/octet-stream")

                  response (-> (response/response (io/input-stream body))
                               (response/content-type content-type)
                               (response/header "Cache-Control" "max-age=86400, public"))]
              response)
            (response/not-found (str "Logo not found at: " target-url))))

        (catch java.io.IOException e
          (println (str "Proxy error: " (.getMessage e)))
          (internal-server-error "Proxy connection failed.")))
      (response/not-found "Unknown logo key."))))

(defn component [_]
  {:routes
   ["" {}
    ["/proxy/image/:client-id" {:name :proxy/image
                                :get #'proxy-image-handler}]]})
