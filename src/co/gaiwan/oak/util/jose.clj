(ns co.gaiwan.oak.util.jose
  "A data-driven wrapper around nimbus-jose-jwt for Clojure.

  Works with string-string maps."
  (:require
   [co.gaiwan.oak.util.uuid :as uuid])
  (:import
   (com.nimbusds.jose JWSAlgorithm JWSHeader JWSHeader$Builder JWSSigner JWSVerifier)
   (com.nimbusds.jose.crypto ECDSASigner ECDSAVerifier Ed25519Signer Ed25519Verifier RSASSASigner RSASSAVerifier)
   (com.nimbusds.jose.jwk ECKey JWK OctetKeyPair RSAKey)
   (com.nimbusds.jose.jwk.gen ECKeyGenerator OctetKeyPairGenerator RSAKeyGenerator)
   (com.nimbusds.jwt JWTClaimsSet SignedJWT)))

(set! *warn-on-reflection* true)

(defn jwk->map
  "Converts a nimbus JWK object into a Clojure map."
  [^JWK jwk]
  (when jwk
    (into {} (.toJSONObject jwk))))

(defn map->jwk
  "Converts a Clojure map into a nimbus JWK object."
  ^JWK [jwk-map]
  (JWK/parse ^java.util.Map jwk-map))

(defn map->claims
  "Converts a Clojure map of claims into a JWTClaimsSet object."
  ^JWTClaimsSet [claims-map]
  (JWTClaimsSet/parse ^java.util.Map claims-map))

(defn claims->map
  "Converts a JWTClaimsSet object into a Clojure map of claims."
  [^JWTClaimsSet claims]
  (when claims
    (into {} (.toJSONObject claims))))

(defmulti new-jwk
  "Generates a new JWK (JSON Web Key) as a Clojure map.
  Dispatches on the \"kty\" (Key Type) value in the input spec map."
  (fn [jwk] (get jwk "kty")))

(defmethod new-jwk "RSA" [{:strs [kid size alg]}]
  (-> (RSAKeyGenerator. ^int size)
      (.algorithm (JWSAlgorithm/parse ^String alg))
      (.keyID (or kid (uuid/random-compacted-uuid)))
      (.generate)
      (jwk->map)))

(defmethod new-jwk "EC" [{:strs [kid crv alg]}]
  (-> (ECKeyGenerator. (com.nimbusds.jose.jwk.Curve/parse crv))
      (.algorithm (JWSAlgorithm/parse ^String alg))
      (.keyID (or kid (uuid/random-compacted-uuid)))
      (.generate)
      (jwk->map)))

(defmethod new-jwk "OKP" [{:strs [kid crv]}]
  (-> (OctetKeyPairGenerator. (com.nimbusds.jose.jwk.Curve/parse crv))
      (.keyID (or kid (uuid/random-compacted-uuid)))
      (.generate)
      (jwk->map)))

(comment
  (new-jwk {"kty" "RSA" "alg" "RS256" "size" 2048})
  (new-jwk {"kty" "EC" "alg" "ES256" "crv" "P-256"})
  (new-jwk {"kty" "OKP" "crv" "Ed25519"})
  (JWSAlgorithm/parse "EdDSA")
  )

(defn build-jwt
  "Creates and signs a JWT string.
  Takes a JWK (as a Clojure map) and a map of claims."
  [jwk-map claims-map]
  (let [^JWK jwk          (map->jwk jwk-map)
        ^String alg-str   (or (get jwk-map "alg")
                              (when (= "OKP" (get jwk-map "kty")) "EdDSA"))
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
  (def rsa-key (new-jwk {"kty" "RSA" "alg" "HS384" "size" 2048}))
  (def claims {"sub" "1234567890"
               "name" "John Doe"
               "iss" "https://my-app.com"
               "iat" 1516239022})

  (build-jwt rsa-key claims)
  ;;=> "eyJraWQiOiI3YjY1...<snip>...ZT12YzAifQ.eyJzdW...<snip>...I6MTUxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
  )


(defn parse-header
  "Parses a JWT string and returns its header as a Clojure map."
  [^String jwt-string]
  (let [header ^JWSHeader (.getHeader (SignedJWT/parse jwt-string))]
    (into {} (.toJSONObject header))))

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
                      {:type :jwt-verification/failed
                       :jwt jwt-string
                       :jwk public-jwk-map})))))

(defn public-parts
  "Given a full JWK map including private key info, return only the public key
  parts."
  [jwk-map]
  (-> jwk-map map->jwk .toPublicJWK jwk->map))

(comment
  (def rsa-key (new-jwk {"kty" "RSA" "alg" "RS256" "size" 2048}))
  (def public-rsa-key (public-parts rsa-key))

  (def my-claims {"sub" "user-123" "iss" "my-app"})
  (def my-jwt (build-jwt rsa-key my-claims))

  (parse-verify-jwt public-rsa-key my-jwt)

  (def other-public-key (public-parts (new-jwk {"kty" "RSA" "alg" "RS256" "size" 2048})))

  (parse-verify-jwt other-public-key my-jwt)
  )
