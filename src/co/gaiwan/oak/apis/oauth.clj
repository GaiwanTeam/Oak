(ns co.gaiwan.oak.apis.oauth
  "OAuth 2.1 authorization and token exchange"
  (:require
   [clojure.string :as str]
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.domain.jwk :as jwk]
   [co.gaiwan.oak.domain.oauth-authorization :as oauth-authorization]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]
   [co.gaiwan.oak.domain.oauth-code :as oauth-code]
   [co.gaiwan.oak.domain.refresh-token :as refresh-token]
   [co.gaiwan.oak.domain.scope :as scope]
   [co.gaiwan.oak.lib.form :as form]
   [co.gaiwan.oak.util.hash :as hash]
   [co.gaiwan.oak.util.jose :as jose]
   [co.gaiwan.oak.util.routing :as routing]
   [lambdaisland.hiccup.middleware :as hiccup-mw]
   [lambdaisland.uri :as uri]))

(defn auth-error-html [extra-info]
  [:<>
   [:h1 "Oh no! Something went wrong."]
   [:hr]
   [:p "It looks like the link you clicked to sign in isn't quite right. Don't worry, this is usually a simple fix."]
   [:p "The application you're trying to use did not pass along all information needed to let you log in. This could be because of a broken link or a typo."]
   [:p "What you can do:"
    [:ul
     [:li "Head back to the application and try clicking the sign-in button again."]
     [:li "If the issue continues, please reach out to the application's support team. They'll know exactly what to do!"]]]
   [:p "We apologize for the inconvenience!"]
   [:hr]
   [:details
    [:summary "Technical information"]
    extra-info]])

(defn permission-dialog-html [oauth-client requested-scopes params]
  (let [{:keys [client_id redirect_uri response_type scope state code_challenge code_challenge_method]} params]
    [:<>
     [:p (:oauth-client/client-name oauth-client) " wants to access your account."]
     [:p "This will allow " (:oauth-client/client-name oauth-client) " to:"]
     [:ul
      (for [s requested-scopes]
        [:li (scope/desc s)])]
     [form/form {:method "post"}
      [:input {:type "hidden", :name "client_id", :value client_id}]
      [:input {:type "hidden", :name "redirect_uri", :value redirect_uri}]
      [:input {:type "hidden", :name "response_type", :value response_type}]
      [:input {:type "hidden", :name "scope", :value scope}]
      [:input {:type "hidden", :name "code_challenge", :value code_challenge}]
      [:input {:type "hidden", :name "code_challenge_method", :value code_challenge_method}]
      (when state [:input {:type "hidden", :name "state", :value state}])
      [:button {:type "submit", :name "allow", :value "true"} "Allow"]
      [:button {:type "submit", :name "allow", :value "false"} "Cancel"]]]))

(defn error-html-response [message]
  {:status 400
   :html/head [:title "Something went wrong"]
   :html/body [auth-error-html message]})

(defn error-redirect-response [redirect-uri kvs]
  {:status 302
   :headers {"Location" (str (uri/assoc-query* redirect-uri kvs))}})

(defn pre-client-checks
  "Initial checks, before we have a client+redirect to send the user back to"
  [{:keys [redirect_uri client_id]}]
  (cond
    (not redirect_uri)
    (error-html-response "OAuth authorize redirect is missing the redirect_uri query parameter in the URL.")

    (not client_id)
    (error-html-response "OAuth authorize redirect is missing the client_id query parameter in the URL.")))

(defn client-checks
  "We've looked up the client in the database, check it against the parameters"
  [oauth-client {:keys [redirect_uri response_type scope state]}]
  (let [requested-scopes (scope/scope-set scope)
        client-scopes (scope/scope-set (:oauth-client/scope oauth-client))
        error-redirect-response (partial error-redirect-response redirect_uri)]
    (cond
      ;; OAuth 2.1, match redirect_uri exactly, not just prefix
      (not (some #{redirect_uri} (:oauth-client/redirect-uris oauth-client)))
      (error-html-response "Invalid redirect_uri for this client")

      (not response_type)
      (error-redirect-response
       {:error "invalid_request"
        :error_description "Missing response type"
        :state state})

      (not (some #{response_type} (:oauth-client/response-types oauth-client)))
      (error-redirect-response
       {:error "unsupported_response_type"
        :error_description (str "Response type `" response_type "` not supported for this client")
        :state state})

      (empty? requested-scopes)
      (error-redirect-response
       {:error "invalid_scope"
        :error_description "Missing scope parameter"
        :state state})

      (not (scope/subset? requested-scopes client-scopes))
      (error-redirect-response
       {:error "invalid_scope"
        :error_description
        (str "Scopes " (seq (remove client-scopes requested-scopes))
             " not valid for this client")
        :state state}))))

(defn identity-checks
  "Check if there's an authenticated user in the session, if not, redirect to the
  login page"
  [req session params]
  (when (not (:identity session))
    {:status 302
     :headers {"Location" (routing/path-for req :auth/login)}
     :session (assoc session :redirect-after-login
                     (str (uri/assoc-query* (routing/path-for req :oauth/authorize) params)))}))

(defn permission-dialog-response
  "Ask the user permission for the requested scopes"
  [oauth-client scope params]
  {:status 200
   :html/body [permission-dialog-html oauth-client (str/split scope #"\s+") params]})

(defn authorize-response
  "Happy path, store a code for later token exchange, and send the user back to
  the Relying Party"
  [db oauth-client identity params]
  (let [{:keys [redirect_uri state scope code_challenge code_challenge_method]} params
        code (oauth-code/create! db {:client-id (:oauth-client/id oauth-client)
                                     :identity-id identity
                                     :scope scope
                                     :code-challenge code_challenge
                                     :code-challenge-method code_challenge_method})]
    {:status 302
     :headers {"Location" (str (uri/assoc-query* redirect_uri {:code code :state state}))}}))

(defn GET-authorize
  {:summary "Authorize a client"
   :description "Starting point of the oauth flow, request authorization."
   :parameters {:query [:map
                        [:client_id {:optional true} string?]
                        [:redirect_uri {:optional true} string?]
                        [:response_type {:optional true} string?]
                        [:scope {:optional true} string?]
                        [:state {:optional true} string?]
                        [:code_challenge {:optional true} string?]
                        [:code_challenge_method {:optional true} string?]]}}
  [{:keys [db parameters session] :as req}]
  (let [{:keys [client_id scope] :as params} (:query parameters)
        identity-id (:identity session)]
    (or
     (pre-client-checks params)
     (if-let [oauth-client (oauth-client/find-by-client-id db client_id)]
       (or
        (client-checks oauth-client params)
        (identity-checks req session params)
        (if (oauth-authorization/exists? db {:client-id (:oauth-client/id oauth-client)
                                             :identity-id identity-id
                                             :scope scope})
          (authorize-response db oauth-client identity-id params)
          (permission-dialog-response oauth-client scope params)))
       (error-html-response (str "Client not found for client_id=" client_id))))))

(defn POST-authorize
  {:summary "Authorize a client, submit authorization dialog"
   :no-doc true
   :parameters {:form [:map
                       [:client_id string?]
                       [:redirect_uri string?]
                       [:response_type string?]
                       [:scope string?]
                       [:state {:optional true} string?]
                       [:code_challenge {:optional true} string?]
                       [:code_challenge_method {:optional true} string?]
                       [:allow boolean?]]}}
  [{:keys [db parameters session] :as req}]
  (let [{:keys [allow client_id redirect_uri scope state] :as params} (:form parameters)
        identity-id (:identity session)
        error-redirect-response (partial error-redirect-response redirect_uri)]
    (or
     (pre-client-checks params)
     (if-let [oauth-client (oauth-client/find-by-client-id db client_id)]
       (or
        (client-checks oauth-client params)
        (identity-checks req session params)
        (if allow
          (do
            (oauth-authorization/create!
             db
             {:client-id (:oauth-client/id oauth-client)
              :identity-id identity-id
              :scope scope})
            (authorize-response db oauth-client identity-id params))
          (error-redirect-response {:error "access_denied" :state state})))
       (error-html-response (str "Client not found for client_id=" client_id))))))

(defn GET-authorization-server-metadata
  {:summary "OAuth 2.0 Authorization Server Metadata"
   :description "Returns OAuth 2.0 authorization server metadata as specified in RFC 8414"
   :responses
   {200
    {:description "Authorization server metadata"
     :content
     {"application/json"
      {:schema [:map
                [:issuer string?]
                [:authorization_endpoint string?]
                [:token_endpoint string?]
                [:response_types_supported [:vector string?]]
                [:grant_types_supported {:optional true} [:vector string?]]
                [:scopes_supported {:optional true} [:vector string?]]
                [:jwks_uri {:optional true} string?]
                [:code_challenge_methods_supported {:optional true} [:vector string?]]]}}}}}
  [{:keys [request-method uri scheme headers] :as req}]
  (let [host (get headers "host")
        base-url (str (name scheme) "://" host)
        issuer base-url]
    {:status 200
     :body
     {:issuer issuer
      :authorization_endpoint (str base-url (routing/path-for req :oauth/authorize))
      :token_endpoint (str base-url (routing/path-for req :oauth/token))
      :response_types_supported oauth-client/valid-response-types
      :grant_types_supported oauth-client/valid-grant-types
      :scopes_supported (vec (keys scope/openid-scopes))
      :code_challenge_methods_supported oauth-client/valid-code-challenge-methods
      :jwks_uri (str base-url (routing/path-for req :jwks/jwks))}}))

(defn new-exp-time
  "JWT expiration time"
  []
  (+ (System/currentTimeMillis)
     (* (config/get :jwt/exp-time-seconds) 1000)))

(defn token-claims
  "Generate JWT claims for an access token"
  [identity-id client-id scope]
  {:pre [identity-id client-id scope]}
  {"sub" (str identity-id)
   "aud" client-id
   "scope" scope
   "exp" (new-exp-time)})

(defn update-exp
  "Reset the expiration claim to a new future timestamp based on :jwt/exp-time"
  [claims]
  (assoc claims "exp" (new-exp-time)))

(defn error-response [error error-description]
  {:status 400
   :body {:error error
          :error_description error-description}})

(defn handle-authorization-code-grant
  "Token exchange: handle `code` grant type"
  [db params]
  (let [{:keys [client_id client_secret code code_verifier]} params]
    (cond
      (not code)
      (error-response "invalid_request" "Missing code parameter")

      (not client_id)
      (error-response "invalid_request" "Missing client_id parameter")

      :else
      (if-let [oauth-client (oauth-client/find-by-client-id db client_id)]
        ;; Validate client authentication
        (let [stored-client-secret (:oauth-client/client-secret oauth-client)
              auth-method (:oauth-client/token-endpoint-auth-method oauth-client)
              client-uuid (:oauth-client/id oauth-client)]
          (cond
            (and (= auth-method "client_secret_basic") (not client_secret))
            (error-response "invalid_client" "Client authentication required")

            (and client_secret (not= client_secret stored-client-secret))
            (error-response "invalid_client" "Invalid client credentials")

            :else
            ;; Look up the authorization code
            (if-let [code-entity (oauth-code/find-one db code client-uuid)]
              (let [{:oauth-code/keys [identity-id scope code-challenge code-challenge-method]} code-entity
                    code-verifier-valid? (when (= code-challenge-method "S256")
                                           (and code_verifier
                                                (= code-challenge (hash/sha256-base64url code_verifier))))]

                (when (and code-challenge (not code-verifier-valid?))
                  (error-response "invalid_grant" "Invalid code verifier"))
                (if-let [default-jwk (:jwk/full-key (jwk/default-key db))]
                  (let [claims (token-claims identity-id client_id scope)
                        access-token (jose/build-jwt default-jwk claims)
                        refresh-token (refresh-token/create! db {:client-id client-uuid :jwt-claims (dissoc claims "exp")})]

                    ;; Delete the used code
                    (oauth-code/delete! db code client-uuid)

                    ;; Return token response with refresh token
                    {:status 200
                     :body {:access_token access-token
                            :token_type "Bearer"
                            :expires_in 3600
                            :refresh_token refresh-token
                            :scope scope}})
                  (error-response "server_error" "No default JWK configured")))
              (error-response "invalid_grant" "Invalid authorization code"))))
        (error-response "invalid_client" "Client not found")))))

(defn handle-refresh-token-grant
  "Token exchange: handle `refresh_token` grant type"
  [db params]
  (let [{:keys [client_id client_secret refresh_token]} params]
    (cond
      (not refresh_token)
      (error-response "invalid_request" "Missing refresh_token parameter")

      (not client_id)
      (error-response "invalid_request" "Missing client_id parameter")

      :else
      (if-let [oauth-client (oauth-client/find-by-client-id db client_id)]
        ;; Validate client authentication
        (let [stored-client-secret (:oauth-client/client-secret oauth-client)
              auth-method (:oauth-client/token-endpoint-auth-method oauth-client)
              client-uuid (:oauth-client/id oauth-client)]
          (cond
            (and (= auth-method "client_secret_basic") (not client_secret))
            (error-response "invalid_client" "Client authentication required")

            (and client_secret (not= client_secret stored-client-secret))
            (error-response "invalid_client" "Invalid client credentials")

            :else
            (if-let [refresh-token-entity (refresh-token/find-one db refresh_token client-uuid)]
              (let [jwt-claims (:refresh-token/jwt-claims refresh-token-entity)]
                (if-let [default-jwk (:jwk/full-key (jwk/default-key db))]
                  (let [access-token (jose/build-jwt default-jwk (update-exp jwt-claims))
                        new-refresh-token (refresh-token/create! db
                                                                 {:client-id client-uuid
                                                                  :jwt-claims jwt-claims})]

                    ;; Delete the used refresh token
                    (refresh-token/delete! db refresh_token client-uuid)

                    ;; Return token response with new refresh token
                    {:status 200
                     :body {:access_token access-token
                            :token_type "Bearer"
                            :expires_in 3600
                            :refresh_token new-refresh-token
                            :scope (get jwt-claims "scope")}})

                  (error-response "server_error" "No default JWK configured")))
              (error-response "invalid_grant" "Invalid refresh token"))))
        (error-response "invalid_client" "Client not found")))))

(defn POST-exchange-token
  {:summary "Exchange an authorization code or refresh token for access tokens"
   :description "OAuth 2.1 token endpoint for authorization code and refresh token grants with PKCE"
   :parameters
   {:form
    [:map
     [:grant_type {:optional true} string?]
     [:code {:optional true} string?]
     [:redirect_uri {:optional true} string?]
     [:client_id {:optional true} string?]
     [:client_secret {:optional true} string?]
     [:code_verifier {:optional true} string?]
     [:refresh_token {:optional true} string?]]}}
  [{:keys [db parameters]}]
  (let [{:keys [grant_type] :as params} (:form parameters)]

    ;; Validate required parameters
    (cond
      (not grant_type)
      (error-response "invalid_request" "Missing grant_type parameter")

      (= grant_type "authorization_code")
      (handle-authorization-code-grant db params)

      (= grant_type "refresh_token")
      (handle-refresh-token-grant db params)

      :else
      (error-response "unsupported_grant_type" "Only authorization_code and refresh_token grant types are supported"))))

(comment
  ;; Test token claims generation
  (token-claims "user123" "client456" "read write")

  ;; Test error response function
  (let [error-response (fn [error error-description]
                         {:status 400
                          :body {:error error
                                 :error_description error-description}})]
    (error-response "invalid_request" "Missing parameter")))

  ;; Test refresh token creation (requires proper DB connection)
  ;; (refresh-token/create! db {:client-id "client123" :jwt-claims {"sub" "user123" "scope" "read"}})

  ;; Test refresh token lookup (requires proper DB connection)
  ;; (refresh-token/find-one db "token123" "client123")

  ;; Test refresh token deletion (requires proper DB connection)
  ;; (refresh-token/delete! db "token123" "client123"))

(defn component [opts]
  {:routes
   [["/.well-known/oauth-authorization-server" {:name :oauth/authorization-server-metadata
                                                :get #'GET-authorization-server-metadata}]
    ["/oauth" {:openapi {:tags ["oauth"]}}
     ["/authorize" {:name :oauth/authorize
                    :middleware [hiccup-mw/wrap-render]
                    :get #'GET-authorize
                    :post #'POST-authorize}]
     ["/token" {}
      ["" {:name :oauth/token
           :post #'POST-exchange-token}]
      #_["/revoke" {:post {}}]]

     #_["/userinfo" {:get {}}]

     #_["/client" {}
        ["/register"
         ["" {:post {:handler #'POST-register-client}}]
         ["/:client-id" {:get {}
                         :put {}
                         :delete {}}]]]]]})

(comment
  (user/restart! :apis/oauth))
