(ns co.gaiwan.oauth-tester.impl
  "OAuth tester tool implementation"
  (:require
   [charred.api :as charred]
   [clojure.string :as str]
   [hato.client :as hato]
   [lambdaisland.uri :as uri]))

(require 'co.gaiwan.oak.lib.hato-charred)

(defn fetch [ctx req]
  (hato/request (cond-> (merge {:as :auto
                                :throw-exceptions? false} req)
                  (not (:request-method req))
                  (assoc :request-method :get)
                  (and (:server-url ctx) (:path req))
                  (assoc :url (-> (str (:server-url ctx)
                                       (:path req))
                                  #_(uri/assoc-query* (:query-params req))
                                  str))
                  #_#_:->
                  (assoc-in [:headers "host"]
                            (str
                             (assoc (uri/uri (:server-url ctx))
                                    :path nil :query nil))))))

(defn fetch-rfc8414-metadata
  "Retrieve OAuth server configuration using .well-known endpoint

  RFC 8414 - OAuth 2.0 Authorization Server Metadata
  "
  [ctx]
  (fetch ctx {:path "/.well-known/oauth-authorization-server"}))

(comment
  (def oak-url "http://localhost:4800")
  (def kc-url "http://localhost:8080/realms/master")
  (fetch-rfc8414-metadata {:server-url oak-url})
  (fetch-rfc8414-metadata kc-url))

(defn authorization-endpoint-rejects-implicit-grant
  {:doc "The server rejects requests with `response_type=token`"
   :description "The Implicit Grant is no longer considered secure, and is being removed in OAuth 2.1."
   :categories #{:authorization-endpoint}
   :references ["http://datatracker.ietf.org/doc/html/rfc9700#name-implicit-grant"]
   :severity :warn}
  [ctx]
  (let [{:keys [status headers body]}
        (fetch ctx {:path (:authorization-endpoint ctx)
                    :query-params
                    {:client_id (:client-id ctx)
                     :client_secret (:client-secret ctx)
                     :redirect_uri (:redirect-uri ctx)
                     :response_type "token"
                     :scope (:scope ctx)
                     :state "123"
                     :code_challenge "xxx"
                     :code_challenge_method "S256"}})]
    (if (= 302 status)
      (let [location (uri/uri (get headers "location"))
            query-map (uri/query-map location)
            fragment-map (uri/query-string->map (:fragment location))
            error (or (:error query-map) (:error fragment-map))]
        (cond
          (#{"unauthorized_client" "unsupported_response_type"} error)
          {:result :ok}
          error
          {:result :warn
           :message (str "Expected error=unauthorized_client or error=unauthorized_client, got" error)}
          :else
          {:result :fail
           :message (str "Expected error in query or fragment, got " location)}))
      {:result :fail
       :message (str "Expected 302 with error param, got " status)})))

(defn validate-bad-client-id-response
  "Helper to validate authorization endpoint error responses for invalid client_id"
  [{:keys [status headers body] :as response}]
  (cond
    (<= 400 status 499)
    (if (and (get headers "content-type")
             (str/includes? (get headers "content-type") "text/html"))
      {:result :ok
       :message (str "Correctly returned " status " with text/html content type")}
      {:result :fail
       :message (str "Expected text/html content type for " status " response, got " (get headers "content-type"))
       :http/headers headers})

    (= 200 status)
    {:result :warn
     :message "Server returned 200 OK for client error - should return 4xx client error instead"
     :http/status status
     :http/headers headers}

    (= 302 status)
    {:result :fail
     :message "Server incorrectly returned 302 redirect for invalid client_id - cannot redirect without valid client_id"
     :http/status status
     :http/headers headers}

    :else
    {:result :fail
     :message (str "Expected 4xx client error response, got " status)
     :http/status status
     :http/headers headers}))

(defn validate-bad-redirect-uri-response
  "Helper to validate authorization endpoint responses for invalid redirect_uri"
  [{:keys [status headers body] :as response} expected-redirect-uri]
  (cond
    (<= 400 status 499)
    (if (and (get headers "content-type")
             (str/includes? (get headers "content-type") "text/html"))
      {:result :ok
       :message (str "Correctly returned " status " with text/html content type")}
      {:result :fail
       :message (str "Expected text/html content type for " status " response, got " (get headers "content-type"))
       :http/headers headers})

    (= 200 status)
    {:result :warn
     :message "Server returned 200 OK for redirect_uri error - should return 4xx client error instead"
     :http/status status
     :http/headers headers}

    (= 302 status)
    (let [location (uri/uri (get headers "location"))
          actual-query (:query location)]
      (if (= (str location) expected-redirect-uri)
        {:result :ok
         :message (str "Correctly redirected to " expected-redirect-uri
                       (when actual-query (str " with query params: " actual-query)))}
        {:result :fail
         :message (str "Redirect location " (str location)
                       " does not match expected " expected-redirect-uri)
         :http/headers headers}))

    :else
    {:result :fail
     :message (str "Expected 4xx client error or 302 redirect, got " status)
     :http/status status
     :http/headers headers}))

(defn authorization-endpoint-requires-client-id
  {:doc "The server rejects requests with missing client_id parameter"
   :description "Authorization requests must include a valid client_id parameter. According to OAuth 2.0 spec, this should return a 4xx client error with text/html content."
   :categories #{:authorization-endpoint}
   :references ["https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.1"
                "https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2.1"]
   :severity :error}
  [ctx]
  (validate-bad-client-id-response
   (fetch ctx {:path (:authorization-endpoint ctx)
               :query-params
               {:redirect_uri (:redirect-uri ctx)
                :response_type "code"
                :scope (:scope ctx)
                :state "123"
                :code_challenge "xxx"
                :code_challenge_method "S256"}})))

(defn authorization-endpoint-rejects-invalid-client-id
  {:doc "The server rejects requests with invalid client_id parameter"
   :description "Authorization requests with non-existent client_id should be rejected with a 4xx client error and text/html content."
   :categories #{:authorization-endpoint}
   :references ["https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.1"
                "https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2.1"]
   :severity :error}
  [ctx]
  (validate-bad-client-id-response
   (fetch ctx {:path (:authorization-endpoint ctx)
               :query-params
               {:client_id "invalid-client-id-that-does-not-exist"
                :redirect_uri (:redirect-uri ctx)
                :response_type "code"
                :scope (:scope ctx)
                :state "123"
                :code_challenge "xxx"
                :code_challenge_method "S256"}})))

(defn authorization-endpoint-requires-redirect-uri
  {:doc "The server rejects requests with missing redirect_uri parameter"
   :description "Authorization requests must include a redirect_uri parameter. Server may return 4xx error or 302 redirect to the registered redirect_uri."
   :categories #{:authorization-endpoint}
   :references ["https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.1"
                "https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2.1"]
   :severity :error}
  [ctx]
  (validate-bad-redirect-uri-response
   (fetch ctx {:path (:authorization-endpoint ctx)
               :query-params
               {:client_id (:client-id ctx)
                :response_type "code"
                :scope (:scope ctx)
                :state "123"
                :code_challenge "xxx"
                :code_challenge_method "S256"}})
   (:redirect-uri ctx)))

(defn authorization-endpoint-rejects-redirect-uri-with-suffix
  {:doc "The server rejects requests with redirect_uri that has additional suffix"
   :description "Authorization requests must use exact redirect_uri match. Additional suffixes should be rejected with 4xx error or redirect to registered URI."
   :categories #{:authorization-endpoint}
   :references ["https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.1"
                "https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2.1"]
   :severity :error}
  [ctx]
  (validate-bad-redirect-uri-response
   (fetch ctx {:path (:authorization-endpoint ctx)
               :query-params
               {:client_id (:client-id ctx)
                :redirect_uri (str (:redirect-uri ctx) "/extra/path")
                :response_type "code"
                :scope (:scope ctx)
                :state "123"
                :code_challenge "xxx"
                :code_challenge_method "S256"}})
   (:redirect-uri ctx)))

(defn authorization-endpoint-rejects-non-matching-redirect-uri
  {:doc "The server rejects requests with non-matching redirect_uri parameter"
   :description "Authorization requests must use a registered redirect_uri. Non-matching URIs should be rejected with 4xx error or redirect to registered URI."
   :categories #{:authorization-endpoint}
   :references ["https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.1"
                "https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2.1"]
   :severity :error}
  [ctx]
  (validate-bad-redirect-uri-response
   (fetch ctx {:path (:authorization-endpoint ctx)
               :query-params
               {:client_id (:client-id ctx)
                :redirect_uri "https://malicious-site.com/callback"
                :response_type "code"
                :scope (:scope ctx)
                :state "123"
                :code_challenge "xxx"
                :code_challenge_method "S256"}})
   (:redirect-uri ctx)))

(defn token-endpoint-supports-client-credentials-grant
  {:doc "The server supports client_credentials grant type"
   :description "The client_credentials grant allows clients to obtain access tokens for their own use, without user involvement."
   :categories #{:token-endpoint}
   :references ["https://datatracker.ietf.org/doc/html/rfc6749#section-4.4"]
   :severity :info}
  [ctx]
  (let [{:keys [status headers body]}
        (fetch ctx {:path "/oauth/token"
                    :request-method :post
                    :headers {"Accept" "application/json"}
                    :as :auto
                    :form-params
                    {:grant_type "client_credentials"
                     :client_id (:client-id ctx)
                     :client_secret (:client-secret ctx)
                     :scope (:scope ctx)}})]
    (if (= 200 status)
      (if (and (contains? body :access_token)
               (contains? body :token_type)
               (= "Bearer" (get body :token_type)))
        {:result :ok
         :message "Client credentials grant successfully returned access token"}
        {:result :fail
         :message (str "Token response missing required fields: " body)})
      {:result :fail
       :message (str "Expected 200 OK response, got " status ": " body)})))

(def all-checks
  [#'authorization-endpoint-rejects-implicit-grant
   #'authorization-endpoint-requires-client-id
   #'authorization-endpoint-rejects-invalid-client-id
   #'authorization-endpoint-requires-redirect-uri
   #'authorization-endpoint-rejects-redirect-uri-with-suffix
   #'authorization-endpoint-rejects-non-matching-redirect-uri
   #'token-endpoint-supports-client-credentials-grant])

(defn run-check [ctx check]
  (let [res (check ctx)]
    (merge (meta check) res)))

(defn run-full-suite
  ([ctx]
   (run-full-suite ctx nil))
  ([ctx report-fn]
   (reduce
    (fn [{:keys [checks ctx]} check]
      (let [res (run-check ctx check)
            ctx (or (:ctx res) ctx)]
        (when report-fn
          (report-fn ctx res))
        {:checks (conj checks res)
         :ctx ctx}))
    {:checks [] :ctx ctx} all-checks)))

(comment
  (run-full-suite {:server-url "http://localhost:4800"
                   :client-id "63c0Q8dnkLD.oak.client"
                   :client-secret "2BFTnYcb8FYmDBhCurZ0o25dhD6oaSQy6"
                   :authorization-endpoint "/oauth/authorize"
                   :redirect-uri "https://example.com/redirect"
                   :scope "openid"}))
