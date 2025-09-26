(ns co.gaiwan.oak.apis.oidc
  "OpenID connect implementation

  These are only the OIDC-specific endpoints like discovery or userinfo. Much of
  the OIDC logic lives inside the OAuth2 implemenation."
  (:require
   [co.gaiwan.oak.domain.identifier :as identifier]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]
   [co.gaiwan.oak.domain.scope :as scope]
   [co.gaiwan.oak.lib.auth-middleware :as auth-mw]
   [co.gaiwan.oak.lib.debug-middleware :as debug]
   [co.gaiwan.oak.util.routing :as routing]))

(defn GET-openid-configuration
  {:summary     "OpenID Connect Discovery"
   :description "Returns OpenID Connect discovery metadata as specified in OpenID Connect Discovery 1.0"
   :responses
   {200
    {:description "OpenID Connect discovery metadata"
     :content
     {"application/json"
      {:schema [:map
                [:issuer string?]
                [:authorization_endpoint string?]
                [:token_endpoint string?]
                [:userinfo_endpoint {:optional true} string?]
                [:jwks_uri string?]
                [:scopes_supported [:vector string?]]
                [:response_types_supported [:vector string?]]
                [:subject_types_supported [:vector string?]]
                [:id_token_signing_alg_values_supported [:vector string?]]
                [:grant_types_supported [:vector string?]]
                [:token_endpoint_auth_methods_supported [:vector string?]]
                [:claims_supported {:optional true} [:vector string?]]
                [:code_challenge_methods_supported [:vector string?]]]}}}}}
  [{:keys [request-method uri scheme headers authority] :as req}]
  {:status 200
   :body
   {:issuer                                (routing/base-url req)
    :authorization_endpoint                (routing/url-for req :oauth/authorize)
    :token_endpoint                        (routing/url-for req :oauth/token)
    :userinfo_endpoint                     (routing/url-for req :oidc/userinfo)
    :jwks_uri                              (routing/url-for req :jwks/jwks)
    :scopes_supported                      (vec (keys scope/openid-scopes))
    :response_types_supported              oauth-client/valid-response-types
    ;; subject_types:
    ;; - public: This provides the same sub (subject) value to all Clients.
    ;; - pairwise: This provides a different sub value to each Client, so as not
    ;;   to enable Clients to correlate the End-User's activities without permission.
    :subject_types_supported               ["public"]
    ;; JWS signing algorithms (alg values) supported by the OP for the ID Token
    ;; to encode the Claims in a JWT [JWT]. The algorithm RS256 MUST be
    ;; included.
    :id_token_signing_alg_values_supported ["RS256"]
    :grant_types_supported                 oauth-client/valid-grant-types
    :token_endpoint_auth_methods_supported oauth-client/valid-token-endpoint-auth-methods
    :claims_supported                      ["sub" "aud" "exp" "iat" "iss"]
    :code_challenge_methods_supported      oauth-client/valid-code-challenge-methods}})

(defn GET-userinfo [{:keys [identity scopes db] :as req}]
  (println (select-keys req [:identity :scopes :headers]))
  (when identity
    (let [identity-id (:identity/id identity)
          userinfo {:sub (str identity-id)}
          userinfo (if-not (some #{"email"} scopes)
                     userinfo
                     (let [email (identifier/find-one
                                  db
                                  {:identity-id identity-id
                                   :type "email"
                                   :primary true})]
                       (assoc userinfo
                              :email (:identifier/value email)
                              :email_verified (:identifier/is-verified email))))]
      {:status 200
       :body userinfo})))

(defn component [opts]
  {:routes
   [["/.well-known/openid-configuration"
     {:name :oauth/openid-configuration
      :get #'GET-openid-configuration}]
    ["/oidc" {:openapi {:tags ["oauth openid"]}}
     ["/userinfo" {:name :oidc/userinfo
                   :get #'GET-userinfo
                   :middleware [auth-mw/wrap-bearer-auth]}]]]})
