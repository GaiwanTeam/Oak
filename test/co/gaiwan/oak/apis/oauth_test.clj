(ns co.gaiwan.oak.apis.oauth-test
  (:require
   [clj-uuid :as uuid]
   [clojure.test :refer :all]
   [co.gaiwan.oak.apis.oauth :as oauth]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.domain.jwk :as jwk]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]
   [co.gaiwan.oak.domain.oauth-code :as oauth-code]
   [co.gaiwan.oak.test-harness :as harness]
   [co.gaiwan.oak.util.hash :as hash]
   [co.gaiwan.oak.util.jose :as jose]))

(use-fixtures :once harness/with-test-database)

(deftest post-exchange-token-happy-path-test
  (harness/ensure-jwk!)
  (testing "POST /token exchange - happy path with PKCE"
    (let [identity-id        (uuid/v7)
          authorization-code "test-auth-code"
          code-verifier      "test-code-verifier-with-sufficient-length-for-pkce"
          code-challenge     (hash/sha256-base64url code-verifier)
          scope              "openid profile"
          redirect-uri       "https://example.com/callback"]

      (identity/create! harness/*db* {:id identity-id})

      (let [oauth-client        (oauth-client/create! harness/*db* {:client-name "Test Client"})
            client-uuid         (:oauth-client/id oauth-client)
            oauth-client-id     (:oauth-client/client-id oauth-client)
            oauth-client-secret (:oauth-client/client-secret oauth-client)
            created-code        (oauth-code/create! harness/*db*
                                                    {:client-id             client-uuid
                                                     :identity-id           identity-id
                                                     :scope                 scope
                                                     :redirect-uri          redirect-uri
                                                     :code-challenge        code-challenge
                                                     :code-challenge-method "S256"})
            response            (oauth/POST-exchange-token
                                 {:db         harness/*db*
                                  :parameters {:form {:grant_type    "authorization_code"
                                                      :code          created-code
                                                      :redirect_uri  redirect-uri
                                                      :client_id     oauth-client-id
                                                      :client_secret oauth-client-secret
                                                      :code_verifier code-verifier}}})]

        (is (= 200 (:status response)) "Should return 200 OK")
        (is (contains? (:body response) :access_token) "Should contain access_token")
        (is (= "Bearer" (:token_type (:body response))) "Token type should be Bearer")
        (is (= 3600 (:expires_in (:body response))) "Should expire in 3600 seconds")
        ;; Scope is optional in response if same as requested
        (is (= scope (:scope (:body response))) "Should return the same scope")

        ;; Verify the authorization code was deleted
        (is (nil? (oauth-code/find-one harness/*db* {:code created-code :client-uuid client-uuid}))
            "Authorization code should be deleted after use")))))

(deftest post-exchange-token-refresh-token-test
  (harness/ensure-jwk!)
  (testing "POST /token exchange - refresh token grant"
    (let [identity-id        (uuid/v7)
          authorization-code "test-auth-code-refresh"
          code-verifier      "test-code-verifier-for-refresh-token-test"
          code-challenge     (hash/sha256-base64url code-verifier)
          scope              "openid profile"
          redirect-uri       "https://example.com/callback"]

      (identity/create! harness/*db* {:id identity-id})

      (let [oauth-client        (oauth-client/create! harness/*db* {:client-name "Test Client Refresh"})
            client-uuid         (:oauth-client/id oauth-client)
            oauth-client-id     (:oauth-client/client-id oauth-client)
            oauth-client-secret (:oauth-client/client-secret oauth-client)

            created-code (oauth-code/create! harness/*db*
                                             {:client-id             client-uuid
                                              :identity-id           identity-id
                                              :scope                 scope
                                              :redirect-uri          redirect-uri
                                              :code-challenge        code-challenge
                                              :code-challenge-method "S256"})

            auth-code-response
            (oauth/POST-exchange-token
             {:db         harness/*db*
              :parameters {:form {:grant_type    "authorization_code"
                                  :code          created-code
                                  :redirect_uri  redirect-uri
                                  :client_id     oauth-client-id
                                  :client_secret oauth-client-secret
                                  :code_verifier code-verifier}}})]

        ;; Verify authorization code exchange worked
        (is (= 200 (:status auth-code-response)) "Authorization code exchange should return 200 OK")
        (is (contains? (:body auth-code-response) :access_token) "Should contain access_token")
        (is (contains? (:body auth-code-response) :refresh_token) "Should contain refresh_token")
        (is (= "Bearer" (:token_type (:body auth-code-response))) "Token type should be Bearer")
        (is (= 3600 (:expires_in (:body auth-code-response))) "Should expire in 3600 seconds")

        (let [refresh-token-value (:refresh_token (:body auth-code-response))
              access-token-1      (:access_token (:body auth-code-response))
              refresh-request     {:db         harness/*db*
                                   :parameters {:form {:grant_type    "refresh_token"
                                                       :refresh_token refresh-token-value
                                                       :client_id     oauth-client-id
                                                       :client_secret oauth-client-secret}}}
              refresh-response    (oauth/POST-exchange-token refresh-request)]

          ;; Assertions for refresh token exchange
          (is (= 200 (:status refresh-response)) "Refresh token exchange should return 200 OK")
          (is (contains? (:body refresh-response) :access_token) "Should contain new access_token")
          (is (contains? (:body refresh-response) :refresh_token) "Should contain new refresh_token")
          (is (= "Bearer" (:token_type (:body refresh-response))) "Token type should be Bearer")
          (is (= 3600 (:expires_in (:body refresh-response))) "Should expire in 3600 seconds")

          ;; Verify the new tokens are different from the old ones (token rotation)
          (let [access-token-2  (:access_token (:body refresh-response))
                refresh-token-2 (:refresh_token (:body refresh-response))]
            (is (not= access-token-1 access-token-2) "Access tokens should be different after refresh")
            (is (not= refresh-token-value refresh-token-2) "Refresh tokens should be rotated")

            ;; Test that old refresh token is no longer valid
            (let [invalid-refresh-request  {:db         harness/*db*
                                            :parameters {:form {:grant_type    "refresh_token"
                                                                :refresh_token refresh-token-value
                                                                :client_id     oauth-client-id
                                                                :client_secret oauth-client-secret}}}
                  invalid-refresh-response (oauth/POST-exchange-token invalid-refresh-request)]
              (is (= 400 (:status invalid-refresh-response)) "Used refresh token should be invalid")
              (is (= "invalid_grant" (:error (:body invalid-refresh-response))) "Should return invalid_grant error"))

            ;; Test refresh token with invalid client credentials
            (let [invalid-client-request  {:db         harness/*db*
                                           :parameters {:form {:grant_type    "refresh_token"
                                                               :refresh_token refresh-token-2
                                                               :client_id     oauth-client-id
                                                               :client_secret "wrong-secret"}}}
                  invalid-client-response (oauth/POST-exchange-token invalid-client-request)]
              (is (= 400 (:status invalid-client-response)) "Invalid client secret should be rejected")
              (is (= "invalid_client" (:error (:body invalid-client-response))) "Should return invalid_client error"))))))))

(deftest post-exchange-token-client-credentials-test
  (harness/ensure-jwk!)
  (testing "POST /token exchange - client_credentials grant"
    (let [scope "openid profile"]
      (let [oauth-client        (oauth-client/create! harness/*db* {:client-name "Client Credentials Client"
                                                                    :grant-types ["client_credentials"]})
            oauth-client-id     (:oauth-client/client-id oauth-client)
            oauth-client-secret (:oauth-client/client-secret oauth-client)
            response            (oauth/POST-exchange-token
                                 {:db         harness/*db*
                                  :parameters {:form {:grant_type    "client_credentials"
                                                      :client_id     oauth-client-id
                                                      :client_secret oauth-client-secret
                                                      :scope         scope}}})]

        (is (= 200 (:status response)) "Should return 200 OK")
        (is (contains? (:body response) :access_token) "Should contain access_token")
        (is (= "Bearer" (:token_type (:body response))) "Token type should be Bearer")
        (is (= 3600 (:expires_in (:body response))) "Should expire in 3600 seconds")
        (is (= scope (:scope (:body response))) "Should return the requested scope")

        ;; Client credentials grant should NOT return refresh_token
        (is (not (contains? (:body response) :refresh_token)) "Should not contain refresh_token"))))

  (testing "POST /token exchange - client_credentials grant with invalid client"
    (let [response (oauth/POST-exchange-token
                    {:db         harness/*db*
                     :parameters {:form {:grant_type    "client_credentials"
                                         :client_id     "invalid-client-id"
                                         :client_secret "invalid-secret"
                                         :scope         "openid"}}})]
      (is (= 400 (:status response)) "Should return 400 Bad Request")
      (is (= "invalid_client" (:error (:body response))) "Should return invalid_client error")))

  (testing "POST /token exchange - client_credentials grant with wrong secret"
    (let [oauth-client    (oauth-client/create! harness/*db* {:client-name "Test Client"
                                                              :grant-types ["client_credentials"]})
          oauth-client-id (:oauth-client/client-id oauth-client)
          response        (oauth/POST-exchange-token
                           {:db         harness/*db*
                            :parameters {:form {:grant_type    "client_credentials"
                                                :client_id     oauth-client-id
                                                :client_secret "wrong-secret"
                                                :scope         "openid"}}})]
      (is (= 400 (:status response)) "Should return 400 Bad Request")
      (is (= "invalid_client" (:error (:body response))) "Should return invalid_client error"))))

(comment
  (require 'kaocha.repl)
  (kaocha.repl/run))
