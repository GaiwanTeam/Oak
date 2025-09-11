(ns co.gaiwan.oak.domain.oauth-client
  "Domain layer for oauth-client entities"
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.util.random :as random]
   [malli.core :as m]
   [malli.error :as me]
   [malli.transform :as mt]
   [co.gaiwan.oak.lib.db :as db]))

(def attributes
  [[:id :uuid :primary-key]
   [:client_id :text :unique [:not nil]]
   [:client_secret :text]
   [:client_name :text]
   [:redirect_uris :jsonb]
   [:token_endpoint_auth_method :text] ;; client_secret_post | client_secret_basic
   [:grant_types :jsonb]
   [:response_types :jsonb]
   [:scope :text]

   ;; https://datatracker.ietf.org/doc/html/rfc7591 - OAuth 2.0 Dynamic Client Registration Protocol
   ;; [:client_uri :text]
   ;; [:logo_uri :text]
   ;; [:contacts :jsonb]
   ;; [:tos_uri :text]
   ;; [:policy_uri :text]
   ;; [:jwks_uri :text]
   ;; [:jwks :jsonb]
   ;; [:software_id :text]
   ;; [:software_version :text]
   ])

(def client-secret-default-entropy-bits 192)

(def valid-token-endpoint-auth-methods
  ["client_secret_basic"
   "none"
   ;; "client_secret_post"
   ;; "private_key_jwt"
   ;; "tls_client_auth"
   ])

(def valid-grant-types
  ["authorization_code"
   "refresh_token"
   "password"
   "client_credentials"
   ;; "implicit" ;; no longer supported in OAuth 2.1, not planning to implement this
   ;; "urn:ietf:params:oauth:grant-type:jwt-bearer" ;; see RFC 7523 JWT Bearer Exchange
   ])

(def valid-response-types ["code"])

(def valid-code-challenge-methods
  ["S256" "plain"])

(def ClientOpts
  [:map
   [:client-name :string]
   [:redirect-uris
    ;; public clients are not useful without a redirect uri, but it could make
    ;; sense for specific types of clients (e.g. service to service), and you
    ;; could add the redirect-uri later on.
    {:optional true
     :default []}
    [:sequential :string]]
   [:token-endpoint-auth-method
    {:optional true
     :default "client_secret_basic"}
    (into [:enum] valid-token-endpoint-auth-methods)]
   [:grant-types
    {:optional true
     :default ["authorization_code"]}
    [:sequential (into [:enum] valid-grant-types)]]
   [:response-types
    {:optional true
     :default ["code"]}
    [:sequential (into [:enum] valid-response-types)]]
   [:scope {:optional true
            :default ""}
    :string]])

(defn create! [db opts]
  (if-let [errors (m/explain ClientOpts opts)]
    (throw (ex-info (str "Invalid oauth-client options: " (me/humanize errors))
                    {:category     :invalid
                     :malli/errors errors}))
    (let [coerced-opts (m/coerce ClientOpts opts (mt/default-value-transformer {::mt/add-optional-keys true}))
          {:keys [client-name redirect-uris token-endpoint-auth-method grant-types response-types scope]} coerced-opts]
      (db/insert!
       db
       "oauth_client"
       {:id                         (uuid/v7)
        :client_id                  (str (random/secure-base62-str 64) ".oak.client")
        :client_secret              (random/secure-base62-str client-secret-default-entropy-bits)
        :client_name                client-name
        :redirect_uris              redirect-uris
        :token_endpoint_auth_method token-endpoint-auth-method
        :grant_types                grant-types
        :response_types             response-types
        :scope                      scope}))))

(defn list-all [db]
  (db/execute-honey!
   db
   {:select [:*]
    :from :oauth_client}))

(defn find-by-client-id [db client-id]
  (first
   (db/execute-honey!
    db
    {:select [:*]
     :from :oauth_client
     :where [:= :client_id client-id]})))

(comment
  (create! (user/db) {:client-name "My client"
                      :redirect-uris []}))
