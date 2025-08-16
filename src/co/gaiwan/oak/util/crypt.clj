(ns co.gaiwan.oak.util.crypt
  "Unopionated helpers over java's public/private key encryption"
  (:require
   [clojure.string :as str])
  (:import
   (java.security AlgorithmParameters KeyFactory KeyPair KeyPairGenerator Signature)
   (java.security.interfaces ECKey)
   (java.security.spec ECGenParameterSpec EdECPoint MGF1ParameterSpec PKCS8EncodedKeySpec PSSParameterSpec X509EncodedKeySpec)
   (javax.crypto Mac)))

(defn new-keypair
  "Generates a new KeyPair for the given type and parameters.

  Tries to be reasonably generic while catering for the fact that different
  algorithms have different parameters. For RSA keys, pass in keysize (e.g.
  2048), for EC keys, pass in curve name. For OKP keys, leave blank"
  ^KeyPair [^String type param]
  (let [kpg (KeyPairGenerator/getInstance type)]
    (cond
      (= "EC" type)
      (let [ec-spec (ECGenParameterSpec. ^String param)]
        (.initialize kpg ec-spec))
      (= "RSA" type)
      (.initialize kpg (Long. param)) ;; intentional reflection
      param
      (.initialize kpg param)) ;; intentional reflection
    (.generateKeyPair kpg)))

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

;; There are three distinct ways to designate a given curve used in an EC key
;; - NIST name, used in JWT "alg" field e.g. P-256
;; - IANA name, used to construct a key in Java, e.g. secp256r1
;; - OID (Object id), e.g. 1.2.840.10045.3.1.7, this is what we get back when we inspect a java key

(def ec-oid->nist
  "Map OIDs to NIST curve names"
  {"1.2.840.10045.3.1.7" "P-256"
   "1.3.132.0.34" "P-384"
   "1.3.132.0.35" "P-521"})

(def ec-nist->iana
  "Map NIST curve names to IANA names"
  {"P-256" "secp256r1"
   "P-384" "secp384r1"
   "P-521" "secp521r1"})

(defn ec-curve-oid
  "Get the OID (Object Identifier) for a given EC curve, given a public key"
  ^String [^ECKey key]
  (.getName
   ^ECGenParameterSpec
   (.getParameterSpec (doto (AlgorithmParameters/getInstance "EC")
                        (.init (.getParams key)))
                      ECGenParameterSpec)))

(defn ec-field-size ^long [^ECKey key]
  (.getFieldSize (.getField (.getCurve (.getParams key)))))

(defn encode-ed-point
  "Encodes an EdECPoint into its standard byte-array representation (little-endian y-coord with x-sign bit)."
  ^bytes [^EdECPoint point]
  (let [y (.getY point)
        ;; The public key is the y-coordinate, encoded as a little-endian byte string.
        ;; .toByteArray produces a big-endian array, so we must reverse it.
        y-bytes-le (byte-array (reverse (.toByteArray y)))
        ;; The sign of the x-coordinate is stored in the most significant bit of the final byte.
        x-sign-bit (if (.isXOdd point) 0x80 0)
        last-byte-idx (dec (alength y-bytes-le))]
    (aset-byte y-bytes-le last-byte-idx
               (byte (bit-or (aget y-bytes-le last-byte-idx) x-sign-bit)))
    y-bytes-le))

(defn signer
  "Given a JCA algorithm name, returns a ready-to-use, configured
  instance of either javax.crypto.Mac or java.security.Signature."
  [^String algorithm-name]
  (cond
    ;; Handle HMAC algorithms, which use the Mac class
    (str/starts-with? algorithm-name "Hmac")
    (Mac/getInstance algorithm-name)

    ;; Handle modern RSA-PSS signatures, which require special parameter configuration
    (str/ends-with? algorithm-name "withRSAandMGF1")
    (let [hash-bit-length (re-find #"\d+" algorithm-name)
          hash-name       (str "SHA-" hash-bit-length)
          mgf1-spec       (get {"256" MGF1ParameterSpec/SHA256
                                "384" MGF1ParameterSpec/SHA384
                                "512" MGF1ParameterSpec/SHA512} hash-bit-length)
          salt-length     (/ (Integer/parseInt hash-bit-length) 8)
          pss-spec        (PSSParameterSpec. hash-name "MGF1" mgf1-spec salt-length 1)
          signer          (Signature/getInstance "RSASSA-PSS")]
      (.setParameter signer pss-spec)
      signer)

    ;; Handle all other standard signatures (e.g., SHA256withRSA, SHA256withECDSA)
    :else
    (Signature/getInstance algorithm-name)))

(defn sign
  "Signs a byte-array payload using the given Mac/Signature instance and key. "
  [signer ^Key key ^bytes payload-bytes]
  (cond
    (instance? Mac signer)
    (do
      (.init ^Mac signer key)
      (.doFinal ^Mac signer payload-bytes))

    (instance? Signature signer)
    (do
      (.initSign ^Signature signer (.getPrivate ^KeyPair key))
      (.update ^Signature signer payload-bytes)
      (.sign ^Signature signer))))

(defn verify
  "Verifies a signature.
  - For Mac, the key must be the same SecretKey used for signing.
  - For Signature, the key must be the corresponding PublicKey."
  [signer ^Key key ^bytes payload-bytes ^bytes signature-bytes]
  (cond
    (instance? Mac signer)
    (let [expected-signature (sign signer key payload-bytes)]
      (java.util.Arrays/equals ^bytes expected-signature  signature-bytes))

    (instance? Signature signer)
    (do
      (.initVerify ^Signature signer (.getPublic ^KeyPair key))
      (.update ^Signature signer payload-bytes)
      (.verify ^Signature signer signature-bytes))))
