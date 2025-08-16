(ns co.gaiwan.oak.util.jwk
  "Unopionated helpers for dealing with JWKs"
  (:require
   [co.gaiwan.oak.util.bigint :as bigint]
   [co.gaiwan.oak.util.crypt :as crypt])
  (:import
   (java.io ByteArrayOutputStream)
   (java.security KeyFactory KeyPair)
   (java.security.interfaces ECPrivateKey ECPublicKey EdECPrivateKey EdECPublicKey RSAPrivateCrtKey RSAPublicKey)
   (java.security.spec ECPoint ECPrivateKeySpec ECPublicKeySpec EdECPoint EdECPrivateKeySpec EdECPublicKeySpec RSAPrivateCrtKeySpec RSAPrivateKeySpec RSAPublicKeySpec)
   (javax.crypto SecretKey)
   (javax.crypto.spec SecretKeySpec)
   (sun.security.util DerInputStream DerValue)))

(def alg->java
  {"HS256" "HmacSHA256"
   "HS384" "HmacSHA384"
   "HS512" "HmacSHA512"
   "RS256" "SHA256withRSA"
   "RS384" "SHA384withRSA"
   "RS512" "SHA512withRSA"
   "ES256" "SHA256withECDSA"
   "ES384" "SHA384withECDSA"
   "ES512" "SHA512withECDSA"
   "PS256" "SHA256withRSAandMGF1"
   "PS384" "SHA384withRSAandMGF1"
   "PS512" "SHA512withRSAandMGF1"})

(defprotocol JWKConvertible
  (->jwk-parts [k] "Extracts JWK parameters from a single key object."))

(extend-protocol JWKConvertible
  SecretKey
  (->jwk-parts [key]
    {:kty "oct"
     :k   (-> key .getEncoded base64/url-encode-no-pad)})

  RSAPublicKey
  (->jwk-parts [pub-key]
    {:kty "RSA"
     :e   (-> pub-key .getPublicExponent bigint/b64url)
     :n   (-> pub-key .getModulus bigint/b64url)})

  RSAPrivateCrtKey
  (->jwk-parts [priv-key]
    {:d   (-> priv-key .getPrivateExponent bigint/b64url)
     :p   (-> priv-key .getPrimeP bigint/b64url)
     :q   (-> priv-key .getPrimeQ bigint/b64url)
     :dp  (-> priv-key .getPrimeExponentP bigint/b64url)
     :dq  (-> priv-key .getPrimeExponentQ bigint/b64url)
     :qi  (-> priv-key .getCrtCoefficient bigint/b64url)})

  ECPublicKey
  (->jwk-parts [pub-key]
    (let [w         (.getW pub-key)
          coord-len (quot (+ (crypt/ec-field-size pub-key) 7) 8)]
      {:kty "EC"
       :crv (ec-oid->nist (ec-curve-oid pub-key))
       :x   (bigint/b64url-padded (.getAffineX w) coord-len)
       :y   (bigint/b64url-padded (.getAffineY w) coord-len)}))

  ECPrivateKey
  (->jwk-parts [^ECPrivateKey priv-key]
    (let [d-len  (quot (+ (crypt/ec-field-size priv-key) 7) 8)]
      {:d (bigint/b64url-padded (.getS priv-key) d-len)}))

  ;; Octet Key Pair / Edwards Curve
  EdECPublicKey
  (->jwk-parts [pub-key]
    {:kty "OKP"
     :crv (-> pub-key .getParams .getName)
     :x   (-> pub-key .getPoint crypt/encode-ed-point base64/url-encode-no-pad)})

  EdECPrivateKey
  (->jwk-parts [priv-key]
    (when-let [d-bytes (.orElse (.getBytes priv-key) nil)]
      {:d (base64/url-encode-no-pad d-bytes)}))

  KeyPair
  (->jwk-parts [key-pair]
    (merge
     (->jwk-parts (.getPublic key-pair))
     (some-> key-pair .getPrivate ->jwk-parts)))

  ;; Default case for unsupported key types
  Object
  (->jwk-parts [key]
    (throw (IllegalArgumentException. (str "JWK conversion not implemented for key type: " (class key))))))

(defn jwk->rsa-keypair
  "Constructs an RSA KeyPair from JWK parts."
  [{:keys [n e d p q dp dq qi]}]
  (let [key-factory  (KeyFactory/getInstance "RSA")
        modulus      (bigint/unsigned-from-b64 n)
        pub-exp      (bigint/unsigned-from-b64 e)
        public-spec  (RSAPublicKeySpec. modulus pub-exp)
        public-key   (.generatePublic key-factory public-spec)
        private-key  (when d
                       (if (and p q dp dq qi)
                         ;; If we have all the CRT params, use the more specific (and faster) key spec.
                         (let [private-spec (RSAPrivateCrtKeySpec.
                                             modulus
                                             pub-exp
                                             (bigint/unsigned-from-b64 d)
                                             (bigint/unsigned-from-b64 p)
                                             (bigint/unsigned-from-b64 q)
                                             (bigint/unsigned-from-b64 dp)
                                             (bigint/unsigned-from-b64 dq)
                                             (bigint/unsigned-from-b64 qi))]
                           (.generatePrivate key-factory private-spec))
                         ;; Otherwise, fall back to the basic private key spec.
                         (let [priv-exp (bigint/unsigned-from-b64 d)
                               private-spec (RSAPrivateKeySpec. modulus priv-exp)]
                           (.generatePrivate key-factory private-spec))))]
    (KeyPair. public-key private-key)))

(defn jwk->ec-keypair
  "Constructs an EC KeyPair from JWK parts."
  [{:keys [crv x y d]}]
  (let [iana-curve   (get crypt/ec-nist->iana crv)
        params       (doto (AlgorithmParameters/getInstance "EC")
                       (.init (ECGenParameterSpec. iana-curve)))
        ec-spec      (.getParameterSpec params ECParameterSpec)
        key-factory  (KeyFactory/getInstance "EC")
        point        (ECPoint. (bigint/unsigned-from-b64 x) (bigint/unsigned-from-b64 y))
        public-spec  (ECPublicKeySpec. point ec-spec)
        public-key   (.generatePublic key-factory public-spec)
        private-key  (when d
                       (let [s            (bigint/unsigned-from-b64 d)
                             private-spec (ECPrivateKeySpec. s ec-spec)]
                         (.generatePrivate key-factory private-spec)))]
    (KeyPair. public-key private-key)))

(defn jwk->okp-keypair
  "Constructs an Edwards-Curve KeyPair from JWK parts (RFC 8037)."
  [{:keys [crv x d]}]
  (let [named-param-spec (NamedParameterSpec. crv)
        key-factory      (KeyFactory/getInstance crv)
        ;; Public key is derived from the 'x' parameter which is the compressed point
        x-bytes          (base64/url-decode x)
        last-byte        (aget x-bytes (dec (alength x-bytes)))
        x-odd?           (not (zero? (bit-and last-byte 0x80)))
        y-bytes-le       (aclone x-bytes)
        _                (aset-byte y-bytes-le (dec (alength y-bytes-le)) (byte (bit-and last-byte 0x7F)))
        y-int            (BigInteger. 1 (byte-array (reverse y-bytes-le)))
        point            (EdECPoint. x-odd? y-int)
        public-spec      (EdECPublicKeySpec. named-param-spec point)
        public-key       (.generatePublic key-factory public-spec)
        ;; Private key is derived from the 'd' parameter which is the scalar
        private-key      (when d
                           (let [d-bytes      (base64/url-decode d)
                                 private-spec (EdECPrivateKeySpec. named-param-spec d-bytes)]
                             (.generatePrivate key-factory private-spec)))]
    (KeyPair. public-key private-key)))

(defn jwk->secret-key
  "Constructs a SecretKey from an 'oct' JWK and a JWS algorithm.
  The JWS algorithm (e.g., 'HS256') is required to correctly instantiate the key."
  ^SecretKey [{:keys [kty k alg]}]
  (SecretKeySpec. (base64/url-decode k) (alg->java alg)))

(defn ->jwk
  "Converts to JWK"
  [key-pair]
  (->jwk-parts key-pair))

(defn pubkey->jwk
  "Converts a java.security.KeyPair to JWK data "
  [^KeyPair key-pair]
  (->jwk-parts (.getPublic key-pair)))

(defn jwk->key
  "Turns JWK data (clojure map) into a KeyPair or SecretKey (for oct) Handles RSA
  EC, OKP, and oct key types. Assumes the JWK contains private key components if
  a full keypair is desired."
  [jwk]
  (case (:kty jwk)
    "RSA" (jwk->rsa-keypair jwk)
    "EC"  (jwk->ec-keypair jwk)
    "OKP" (jwk->okp-keypair jwk)
    "oct" (jwk->secret-key jwk)
    (throw (IllegalArgumentException. (str "Unsupported JWK key type: " (:kty jwk))))))




(comment

  (def rsa-jwk
    {:kty "RSA",
     :n   "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cb-hGfe9GtgaR-3LPS9R-7ryudOnPVBSosM-gH8PQ-Isy7v5QSQ9Y-oU2-n_2_d2d5jsyD1W_3mp_B25l4k_tH2W-G3LqfQk_69g-E1xawA6f8z-34p-s",
     :e   "AQAB",
     :d   "X4cTteJY_gn4FYPsXB8rdXix5K_IupGQSCpCjPaa0okcFp0VwTdwt5468V6_eH0ktvPlz27NBfRfaW73qCj0S-pGAU-q3eDT62_d0MA7x7bn-X_e6-G2S_3Rj2R-2LdG8_a4-2PC_2e4v1g0_SAESzVwA_bX-aD5s",
     :p   "83i-7IvMGXoMXCskv73TKr8637FiO7Z27zv8oj6pbWUQyLPQBQxtPVnwD20R-60eTDmD2ujnMt5PoqMrm8Rfm2Q",
     :q   "3dfOR9cuYq-0S-a82oN4oRXI0grJSxGpd-wYVEHy2vMv1g-lTYsCj7FN4Tfi92m3i5rU30D8co_C2roCUEk2Gg",
     :dp  "G4sPXkc6Ya9y8oJW9_ILj4xuppu0lzi_H7VTkS8xj5SdX3coE0oimYwxIi2emTAue0UOa5dpgFGyBJ4c8tQ2geg",
     :dq  "s9lAH9fggBsoFR8Oac2R_E2gw282rT2kGOAhvIllETE1efrA6huUUvMfBcMpn8lqeW6vzznYY5SSQF7pMdC_ag",
     :qi  "AvdY6Old5m21T_lRCUSA_gqMtqrHhFunOqhiGjHNT1U"})

  (def ec-jwk
    {:kty "EC",
     :crv "P-256",
     :x   "f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU",
     :y   "x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0",
     :d   "jpsQnnGQmL-YBIffH1136cspYG6-0iY7X1fCE9-E9LI"})

  (jwk->keypair rsa-jwk)
  (jwk->keypair ec-jwk)

  )


(comment

  (defn check-round-trip [kp]
    (let [jwk (->jwk kp)]
      (= jwk (-> jwk jwk->keypair ->jwk)))
    )
  (.getPublic
   (jwk->keypair (->jwk
                  (crypt/new-keypair "RSA" 2048))))
  (check-round-trip (crypt/new-keypair "RSA" 2048))
  (check-round-trip  (crypt/new-keypair "EC" "secp384r1"))
  (check-round-trip  (crypt/new-keypair "Ed25519" nil))

  (.getPrivate
   (jwk->keypair (pubkey->jwk (crypt/new-keypair "RSA" 2048))))
  (jwk->keypair (->jwk (crypt/new-keypair "EC" "secp384r1")))
  (jwk->keypair (->jwk (crypt/new-keypair "Ed25519" nil)))

  )
