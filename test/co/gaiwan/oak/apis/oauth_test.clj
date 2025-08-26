(ns co.gaiwan.oak.apis.oauth-test
  (:require
   [clojure.test :refer :all]
   [co.gaiwan.oak.apis.oauth :as oauth]
   [co.gaiwan.oak.domain.jwk :as jwk]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]
   [co.gaiwan.oak.domain.oauth-code :as oauth-code]
   [co.gaiwan.oak.lib.db :as db]
   [co.gaiwan.oak.test-harness :as harness]
   [co.gaiwan.oak.util.hash :as hash]
   [co.gaiwan.oak.util.jose :as jose]))

(use-fixtures :once harness/with-test-database)

(deftest post-exchange-token-happy-path-test
  (testing "POST /token exchange - happy path with PKCE"
    (let [identity-id (java.util.UUID/randomUUID)
          authorization-code "test-auth-code"
          code-verifier "test-code-verifier-with-sufficient-length-for-pkce"
          code-challenge (hash/sha256-base64url code-verifier)
          scope "openid profile"
          redirect-uri "https://example.com/callback"]

      ;; Set up test data
      ;; 1. Create identity
      (db/insert! harness/*db* :identity {:id identity-id :type "person"})

      ;; 2. Create OAuth client and get generated credentials
      (let [oauth-client (oauth-client/create! harness/*db*
                                               {:client-name "Test Client"
                                                :token-endpoint-auth-method "client_secret_basic"})
            client-pk (:oauth-client/id oauth-client)
            oauth-client-id (:oauth-client/client-id oauth-client)
            oauth-client-secret (:oauth-client/client-secret oauth-client)]

        ;; 3. Create JWK for token signing
        (jwk/create! harness/*db*
                     (jose/new-jwk {"kty" "RSA" "alg" "RS256" "size" 2048 "kid" "test-key-1"}))

        ;; 4. Create authorization code
        (let [created-code (oauth-code/create! harness/*db*
                                               {:client-id client-pk
                                                :identity-id identity-id
                                                :scope scope
                                                :redirect-uri redirect-uri
                                                :code-challenge code-challenge
                                                :code-challenge-method "S256"})]

          ;; Test the token exchange
          (let [request {:db harness/*db*
                         :parameters {:form {:grant_type "authorization_code"
                                             :code created-code
                                             :redirect_uri redirect-uri
                                             :client_id oauth-client-id
                                             :client_secret oauth-client-secret
                                             :code_verifier code-verifier}}}
                response (oauth/POST-exchange-token request)]

            ;; Assertions
            (is (= 200 (:status response)) "Should return 200 OK")
            (is (contains? (:body response) :access_token) "Should contain access_token")
            (is (= "Bearer" (:token_type (:body response))) "Token type should be Bearer")
            (is (= 3600 (:expires_in (:body response))) "Should expire in 3600 seconds")
            ;; Scope is optional in response if same as requested
            (when-let [response-scope (:scope (:body response))]
              (is (= scope response-scope) "Should return the same scope"))

            ;; Verify the authorization code was deleted
            (is (nil? (oauth-code/find-by-code harness/*db* created-code))
                "Authorization code should be deleted after use")))))))

(comment
  (require 'kaocha.repl)
  (kaocha.repl/run)
  )
