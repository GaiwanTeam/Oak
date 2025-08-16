(ns jwk
  (:require
   [clojure.string :as str])
  (:import
   (java.security.spec X509EncodedKeySpec PKCS8EncodedKeySpec)
   (java.security KeyPair KeyPairGenerator)
   (java.security.interfaces RSAPrivateCrtKey RSAPublicKey)
   (java.security.spec ECGenParameterSpec)
   (java.util Base64)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn base64-encode [^bytes data]
  (.encodeToString (Base64/getEncoder) data))

(defn base64url-encode [^bytes data]
  (-> (Base64/getUrlEncoder)
      (.withoutPadding)
      (.encodeToString data)))

(defn base64url-decode [^String s]
  (.decode (Base64/getUrlDecoder) s))

(defn ^KeyPair generate-keypair
  [^String type ^long keysize]
  (let [^KeyPairGenerator gen (KeyPairGenerator/getInstance type)]
    ;; (.initialize gen (ECGenParameterSpec. "secp256r1"))
    (.generateKeyPair gen)))

(.getAlgorithm
 (.getPublic
  (generate-keypair "EdDSA" 1)))
(.getAlgorithm
 (.getPublic
  (generate-keypair "Ed25519" 1)))
(defn split-64 [^String s]
  (let [len (.length s)]
    (loop [i 0
           chunks []]
      (if (>= i len)
        chunks
        (recur (+ i 64)
               (conj chunks (subs s i (min len (+ i 64)))))))))

(defn encode-pem
  "Encode a key (byte array) with header/footer lines."
  [^bytes key-bytes header footer]
  (str "-----BEGIN " header "-----\n"
       (str/join "\n" (split-64 (base64-encode key-bytes)))
       "\n-----END " footer "-----\n"))

(defn public-key->pem [^RSAPublicKey pub]
  (let [spec (.getEncoded pub)]
    (encode-pem spec "PUBLIC KEY" "PUBLIC KEY")))


(println (public-key->pem (.getPublic (generate-rsa-keypair))))

(defn private-key->pem [^RSAPrivateCrtKey priv]
  (let [spec (.getEncoded priv)] ;; PKCS#8 by default in Java
    (encode-pem spec "PRIVATE KEY" "PRIVATE KEY")))

(defn rsa-keypair->jwk
  "Convert an RSA public/private keypair to a JWK map.
   If private key is nil, returns a public-only JWK."
  [^RSAPublicKey pub ^RSAPrivateCrtKey priv key-id]
  (cond-> {"kty" "RSA"
           "n"   (base64url-encode (.toByteArray (.getModulus pub)))
           "e"   (base64url-encode (.toByteArray (.getPublicExponent pub)))}
    priv (assoc
          "d"  (base64url-encode (.toByteArray (.getPrivateExponent priv)))
          "p"  (base64url-encode (.toByteArray (.getPrimeP priv)))
          "q"  (base64url-encode (.toByteArray (.getPrimeQ priv)))
          "dp" (base64url-encode (.toByteArray (.getPrimeExponentP priv)))
          "dq" (base64url-encode (.toByteArray (.getPrimeExponentQ priv)))
          "qi" (base64url-encode (.toByteArray (.getCrtCoefficient priv))))
    key-id (assoc "kid" key-id)))


(defn keypair->public-bytes
  "Extracts the public key bytes from a KeyPair."
  ^bytes [^KeyPair keypair]
  (.getEncoded (.getPublic keypair)))

(defn keypair->private-bytes
  "Extracts the private key bytes from a KeyPair."
  ^bytes [^KeyPair keypair]
  (.getEncoded (.getPrivate keypair)))

(defn build-keypair
  "Builds a KeyPair from key bytes and algorithm."
  ^KeyPair [^String algorithm ^bytes public-key-bytes ^bytes private-key-bytes]
  (let [^KeyFactory key-factory (KeyFactory/getInstance algorithm)
        public-key (.generatePublic key-factory (X509EncodedKeySpec. public-key-bytes))
        private-key (.generatePrivate key-factory (PKCS8EncodedKeySpec. private-key-bytes))]
    (KeyPair. public-key private-key)))

(defn new-keypair
  "Generates a new KeyPair for the given type and parameters.

  Tries to be reasonably generic while catering for the fact that different
  algorithms have different parameters. For RSA keys, pass in keysize (e.g.
  2048), for EC keys, pass in curve name. For OKP keys, leave blank"
  [^String type param]
  (let [kpg (KeyPairGenerator/getInstance type)]
    (cond
      (= "EC" type)
      (let [ec-spec (ECGenParameterSpec. ^String param)]
        (.initialize kpg ec-spec))
      (= "RSA" type)
      (.initialize kpg (Long. param))
      param
      (.initialize kpg param))
    (.generateKeyPair kpg)))


(comment
  (new-keypair "RSA" 2048)
  (new-keypair "EC" "secp256r1")
  (new-keypair "Ed25519" nil)) ;; OKP

(require 'clojure.data.csv)
(def alg
  (clojure.data.csv/read-csv
   (slurp
    "https://www.iana.org/assignments/jose/web-signature-encryption-algorithms.csv")))

(defn slurp-csv [f]
  (clojure.data.csv/read-csv (slurp f)))

"https://www.iana.org/assignments/jose/web-signature-encryption-header-parameters.csv"
"https://www.iana.org/assignments/jose/web-signature-encryption-algorithms.csv"
(slurp-csv "https://www.iana.org/assignments/jose/web-key-types.csv")
"https://www.iana.org/assignments/jose/web-key-elliptic-curve.csv"
"https://www.iana.org/assignments/jose/web-key-parameters.csv"

(map first
     (filter (fn [[n d _ req]]
               (re-find #"Req|Rec|Opt" req ))
             (next alg)))
