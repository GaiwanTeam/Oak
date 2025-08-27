(ns repl-session.refresh-token
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.apis.oauth :as oauth]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]
   [co.gaiwan.oak.domain.oauth-code :as oauth-code]
   [co.gaiwan.oak.util.hash :as hash]))

(def db user/db)

(def identity-id (uuid/v7))
(def authorization-code "test-auth-code-refresh")
(def code-verifier "test-code-verifier-for-refresh-token-test")
(def code-challenge (hash/sha256-base64url code-verifier))
(def scope "openid profile")
(def redirect-uri "https://example.com/callback")

(identity/create! (db) {:id identity-id})

(def oauth-client
  (oauth-client/create!
   (db)
   {:client-name "Test Client Refresh"
    :token-endpoint-auth-method "client_secret_basic"}))
(def client-pk (:oauth-client/id oauth-client))
(def oauth-client-id (:oauth-client/client-id oauth-client))
(def oauth-client-secret (:oauth-client/client-secret oauth-client))

(def created-code (oauth-code/create! (db)
                                      {:client-id client-pk
                                       :identity-id identity-id
                                       :scope scope
                                       :redirect-uri redirect-uri
                                       :code-challenge code-challenge
                                       :code-challenge-method "S256"}))
(oauth-code/find-by-code (db) created-code)
;; First, exchange authorization code for access token + refresh token
(def auth-code-response
  (oauth/POST-exchange-token {:db (db)
                              :parameters {:form {:grant_type "authorization_code"
                                                  :code created-code
                                                  :redirect_uri redirect-uri
                                                  :client_id oauth-client-id
                                                  :client_secret oauth-client-secret
                                                  :code_verifier code-verifier}}}))

;; Verify authorization code exchange worked
(= 200 (:status auth-code-response))
(contains? (:body auth-code-response) :access_token)
(contains? (:body auth-code-response) :refresh_token)
(= "Bearer" (:token_type (:body auth-code-response)))
(= 3600 (:expires_in (:body auth-code-response)))

(def refresh-token-value (:refresh_token (:body auth-code-response)))
(def access-token-1 (:access_token (:body auth-code-response)))
(def refresh-response (oauth/POST-exchange-token {:db (db) :parameters {:form {:grant_type "refresh_token" :refresh_token refresh-token-value :client_id oauth-client-id :client_secret oauth-client-secret}}}))

;; Assertions for refresh token exchange
(is (= 200 (:status refresh-response)) "Refresh token exchange should return 200 OK")
(is (contains? (:body refresh-response) :access_token) "Should contain new access_token")
(is (contains? (:body refresh-response) :refresh_token) "Should contain new refresh_token")
(is (= "Bearer" (:token_type (:body refresh-response))) "Token type should be Bearer")
(is (= 3600 (:expires_in (:body refresh-response))) "Should expire in 3600 seconds")

;; Verify the new tokens are different from the old ones (token rotation)
(let [access-token-2 (:access_token (:body refresh-response))
      refresh-token-2 (:refresh_token (:body refresh-response))]
  (is (not= access-token-1 access-token-2) "Access tokens should be different after refresh")
  (is (not= refresh-token-value refresh-token-2) "Refresh tokens should be rotated")

  ;; Test that old refresh token is no longer valid
  (let [invalid-refresh-request {:db (db)
                                 :parameters {:form {:grant_type "refresh_token"
                                                     :refresh_token refresh-token-value
                                                     :client_id oauth-client-id
                                                     :client_secret oauth-client-secret}}}
        invalid-refresh-response (oauth/POST-exchange-token invalid-refresh-request)]
    (is (= 400 (:status invalid-refresh-response)) "Used refresh token should be invalid")
    (is (= "invalid_grant" (:error (:body invalid-refresh-response))) "Should return invalid_grant error"))

  ;; Test refresh token with invalid client credentials
  (let [invalid-client-request {:db (db)
                                :parameters {:form {:grant_type "refresh_token"
                                                    :refresh_token refresh-token-2
                                                    :client_id oauth-client-id
                                                    :client_secret "wrong-secret"}}}
        invalid-client-response (oauth/POST-exchange-token invalid-client-request)]
    (is (= 400 (:status invalid-client-response)) "Invalid client secret should be rejected")
    (is (= "invalid_client" (:error (:body invalid-client-response))) "Should return invalid_client error")))
