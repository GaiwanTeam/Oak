(ns co.gaiwan.oak.domain.jwt
  "Utility functions for creating/handling JWTs and JWT claims.

  Both access tokens and ID tokens."
  (:require
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.domain.identifier :as identifier]
   [co.gaiwan.oak.domain.jwk :as jwk]
   [co.gaiwan.oak.domain.scope :as scope]
   [co.gaiwan.oak.util.base64 :as base64]
   [co.gaiwan.oak.util.hash :as hash]
   [co.gaiwan.oak.util.jose :as jose])
  (:import
   (java.util Arrays)))

(set! *warn-on-reflection* true)

(defn at-hash
  "Compute the `at-hash` claim, which is
  base64url(leftmostHalf(hash(access-token)))"
  [access-token hash-alg]
  (let [bytes (hash/hash-bytes hash-alg access-token)
        len   (alength bytes)]
    (base64/url-encode-no-pad (Arrays/copyOfRange bytes 0 (long (/ len 2))))))

(defn at-exp [now]
  (+ (long (/ now 1000)) (config/get :oauth/access-token-exp-time-seconds)))

(defn id-exp [now]
  (+ (long (/ now 1000)) (config/get :openid/id-token-exp-time-seconds)))

(defn iat [now]
  (long (/ now 1000)))

(defn access-token-claims
  "Generate JWT claims for an access token"
  [{:keys [issuer identity-id client-id now scope]}]
  {:pre [identity-id client-id scope]}
  {"iss"   issuer
   "sub"   (str identity-id)
   "aud"   client-id
   "iat"   (iat now)
   "scope" scope
   "exp"   (at-exp now)})

(defn id-token-claims
  "Generate JWT claims for an id token"
  [db {:keys [issuer identity-id client-id now auth-time hash-alg access-token scope]}]
  (let [scopes (scope/scope-set scope)
        claims {"iss"       issuer
                "sub"       (str identity-id)
                "aud"       client-id
                "exp"       (id-exp now)
                "iat"       (iat now)
                "auth_time" auth-time
                "at_hash"   (at-hash access-token hash-alg)}]
    (if-let [email (and (some #{"email"} scopes)
                        (identifier/find-one db {:identity-id identity-id :type "email" :primary true}))]
      (assoc claims
             "email" (:identifier/value email)
             "email_verified" (str (:identifier/is-verified email)))
      claims)))

(defn update-at-claims [claims {:keys [now]}]
  (assoc claims "iat" (iat now) "exp" (at-exp now)))

(defn update-id-claims [claims {:keys [now hash-alg access-token]}]
  (assoc claims
         "iat" (iat now)
         "exp" (at-exp now)
         "at_hash" (at-hash access-token hash-alg)))

(defn parse-verify
  "Lookup the JWK based on the key id (kid) in the token header, then check the
  signature, and return the claims map"
  [db token]
  (let [header (jose/parse-header token)
        kid    (get header "kid")
        jwk    (and kid (:jwk/public-key (jwk/find-one db {:kid kid})))]
    (when jwk
      (jose/parse-verify-jwt jwk token))))

(defn expired? [claims]
  (< (get claims "exp") (/ (System/currentTimeMillis) 1000)))
