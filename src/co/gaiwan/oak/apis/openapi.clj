(ns co.gaiwan.oak.apis.openapi
  (:require [reitit.openapi :as openapi]))

(defn component [opts]
  {:routes ["/openapi.json"
            {:get
             {:no-doc true
              :handler (openapi/create-openapi-handler)
              :openapi {:info {:title "Oak API"
                               :version "1.0.0"}}}}]})
