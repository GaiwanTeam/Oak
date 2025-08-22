(ns co.gaiwan.oak.domain.oauth-client)

(def attributes
  ;; https://datatracker.ietf.org/doc/html/rfc7591
  [[:client_id :text]
   [:client_secret :text]
   [:redirect_uris :jsonb]
   [:token_endpoint_auth_method :text] ;; client_secret_post | client_secret_basic
   [:grant_types :jsonb]
   [:response_types :jsonb]
   [:client_name :text]
   [:client_uri :text]
   [:logo_uri :text]
   [:scope :text]
   [:contacts :jsonb]
   [:tos_uri :text]
   [:policy_uri :text]
   [:jwks_uri :text]
   [:jwks :jsonb]
   [:software_id :text]
   [:software_version :text]
   ])
