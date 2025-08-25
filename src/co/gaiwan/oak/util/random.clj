(ns co.gaiwan.oak.util.random
  "Generate (secure) random things"
  (:require
   [co.gaiwan.oak.util.bigint :as bigint])
  (:import
   (java.math BigInteger)
   (java.security SecureRandom)))

(defn random-bigint [bits]
  {:pre [(pos-int? bits)]}
  (let [bytes (byte-array (Math/ceil (/ bits 8)))]
    (.nextBytes (SecureRandom.) bytes)
    (BigInteger. 1 bytes)))

(defn secure-base62-str
  "Generates a cryptographically secure random alphanumeric (base62) string.
  `bits` is an integer representing the number of bits of entropy, this
  should be a multiple of 8."
  [bits]
  (bigint/bigint->base62 (random-bigint bits)))
