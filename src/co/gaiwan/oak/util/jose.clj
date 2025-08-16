(ns co.gaiwan.oak.util.jose
  "A data-driven wrapper around nimbus-jose-jwt for Clojure."
  (:require
   [clojure.walk :as walk])
  (:import
   (java.util UUID HashMap)
   (com.nimbusds.jose.crypto RSASSASigner RSASSAVerifier ECDSASigner ECDSAVerifier Ed25519Signer Ed25519Verifier)
   (com.nimbusds.jose.jwk ECKey JWK KeyType RSAKey OctetKeyPair)
   (com.nimbusds.jose.jwk.gen ECKeyGenerator RSAKeyGenerator OctetKeyPairGenerator)
   (com.nimbusds.jose JWSAlgorithm JWSHeader JWSSigner JWSVerifier JWSHeader$Builder)
   (com.nimbusds.jwt JWTClaimsSet SignedJWT)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn jwk->map
  "Converts a nimbus JWK object into a Clojure map."
  [^JWK jwk]
  (when jwk
    (->> (.toJSONObject jwk)
         (into {})
         walk/keywordize-keys)))

(defn map->jwk
  "Converts a Clojure map into a nimbus JWK object."
  ^JWK [jwk-map]
  (JWK/parse ^java.util.Map (walk/stringify-keys jwk-map)))

(defn map->claims
  "Converts a Clojure map of claims into a JWTClaimsSet object."
  ^JWTClaimsSet [claims-map]
  (JWTClaimsSet/parse ^java.util.Map (walk/stringify-keys claims-map)))

(defn claims->map
  "Converts a JWTClaimsSet object into a Clojure map of claims."
  [^JWTClaimsSet claims]
  (when claims
    (->> (.toJSONObject claims)
         (into {})
         walk/keywordize-keys)))

(defmulti new-jwk
  "Generates a new JWK (JSON Web Key) as a Clojure map.
  Dispatches on the `:kty` (Key Type) value in the input spec map.
  Supported key types: \"RSA\", \"EC\", \"OKP\"."
  :kty)

(defmethod new-jwk "RSA" [{:keys [size alg]}]
  (-> (RSAKeyGenerator. ^int size)
      (.algorithm (JWSAlgorithm/parse ^String alg))
      (.keyID (str (UUID/randomUUID)))
      (.generate)
      (jwk->map)))

(defmethod new-jwk "EC" [{:keys [crv alg]}]
  (-> (ECKeyGenerator. (com.nimbusds.jose.jwk.Curve/parse crv))
      (.algorithm (JWSAlgorithm/parse ^String alg))
      (.keyID (str (UUID/randomUUID)))
      (.generate)
      (jwk->map)))

(defmethod new-jwk "OKP" [{:keys [crv]}]
  (-> (OctetKeyPairGenerator. (com.nimbusds.jose.jwk.Curve/parse crv))
      (.keyID (str (UUID/randomUUID)))
      (.generate)
      (jwk->map)))

(comment
  (new-jwk {:kty "RSA" :alg "RS256" :size 2048})
  ;;=> {:kty "RSA", :kid "...", :alg "RS256", :n "...", :e "AQAB", :d "...", ...}

  (new-jwk {:kty "EC" :alg "ES256" :crv "P-256"})
  ;;=> {:kty "EC", :kid "...", :alg "ES256", :crv "P-256", :x "...", :y "...", :d "..."}

  (new-jwk {:kty "OKP" :crv "Ed25519"})
  ;;=> {:kty "OKP", :kid "...", :crv "Ed25519", :x "...", :d "..."}
  )

(defn build-jwt
  "Creates and signs a JWT string.
  Takes a JWK (as a Clojure map) and a map of claims."
  [jwk-map claims-map]
  (let [^JWK jwk          (map->jwk jwk-map)
        ^String alg-str   (or (:alg jwk-map)
                              (when (= "OKP" (:kty jwk-map)) "EdDSA"))
        alg               (JWSAlgorithm/parse alg-str)
        ^JWSSigner signer (case (.getValue (.getKeyType jwk))
                            "RSA" (RSASSASigner. ^RSAKey jwk)
                            "EC"  (ECDSASigner. ^ECKey jwk)
                            "OKP" (Ed25519Signer. ^OctetKeyPair jwk))
        header            (-> (JWSHeader$Builder. alg)
                              (.keyID (.getKeyID jwk))
                              (.type (com.nimbusds.jose.JOSEObjectType/JWT))
                              (.build))
        claims            (map->claims claims-map)
        signed-jwt        (SignedJWT. header claims)]
    (.sign signed-jwt signer)
    (.serialize signed-jwt)))

(comment
  (def rsa-key (new-jwk {:kty "RSA" :alg "RS256" :size 2048}))
  (def claims {:sub "1234567890"
               :name "John Doe"
               :iss "https://my-app.com"
               :iat 1516239022})

  (build-jwt rsa-key claims)
  ;;=> "eyJraWQiOiI3YjY1...<snip>...ZT12YzAifQ.eyJzdW...<snip>...I6MTUxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
  )


(defn parse-header
  "Parses a JWT string and returns its header as a Clojure map."
  [^String jwt-string]
  (let [header ^JWSHeader (.getHeader (SignedJWT/parse jwt-string))]
    (->> (.toJSONObject header)
         (into {})
         walk/keywordize-keys)))

(comment
  (def jwt-str "eyJraWQiOiI3YjY1YjY5Ny1kYjU5LTQyYjUtYjEwNC1kZTE2MmI4ZGU5YjAiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaXNzIjoiaHR0cHM6Ly9teS1hcHAuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ.Twj-uT22t8iNn0T4j2i9T3hYpwxv_isA-4g9b4F4jA0B8y9hG2y5aQ_L5f4eG9n3k_a2dF5c6bV7g8hI9oW9eQ")
  (parse-header jwt-str)
  ;;=> {:kid "7b65b697-db59-42b5-b104-de162b8de9b0", :typ "JWT", :alg "RS256"}
  )


(defn parse-verify-jwt
  "Parses and verifies a JWT string using the provided public JWK.
  Returns the claims map if verification succeeds, otherwise throws an exception."
  [public-jwk-map jwt-string]
  (let [public-jwk ^JWK (map->jwk public-jwk-map)
        signed-jwt ^SignedJWT (SignedJWT/parse ^String jwt-string)
        ;; Create a verifier based on the key type
        verifier ^JWSVerifier (case (.getValue (.getKeyType public-jwk))
                                "RSA" (RSASSAVerifier. (.toRSAKey public-jwk))
                                "EC"  (ECDSAVerifier. (.toECKey public-jwk))
                                "OKP" (Ed25519Verifier. (.toOctetKeyPair public-jwk)))]
    (if (.verify signed-jwt verifier)
      (claims->map (.getJWTClaimsSet signed-jwt))
      (throw (ex-info "JWT Signature verification failed."
                      {:jwt jwt-string
                       :jwk public-jwk-map})))))

(comment
  ;; We need the *public* part of the key to verify.
  ;; The .toPublicJWK method on the nimbus object handles this for us.
  (def rsa-key-with-private (new-jwk {:kty "RSA" :alg "RS256" :size 2048}))
  (def public-rsa-key (-> rsa-key-with-private map->jwk .toPublicJWK jwk->map))

  (def my-claims {:sub "user-123" :iss "my-app"})
  (def my-jwt (build-jwt rsa-key-with-private my-claims))

  ;; Verification should succeed
  (parse-verify-jwt public-rsa-key my-jwt)
  ;;=> {:sub "user-123", :iss "my-app"}

  ;; --- Example of a failure ---
  (def other-public-key (-> (new-jwk {:kty "RSA" :alg "RS256" :size 2048}) map->jwk .toPublicJWK jwk->map))

  (try
    (parse-verify-jwt other-public-key my-jwt)
    (catch Exception e
      (ex-data e)))
  ;;=> {:jwt "ey...", :jwk {:kty "RSA", :kid "...", ...}}
  )
