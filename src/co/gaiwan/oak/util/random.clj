(ns co.gaiwan.oak.util.random
  (:import
   (java.math BigInteger)
   (java.security SecureRandom)))

(def ^:private base62-chars "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")
(def ^:private base62-base (BigInteger/valueOf 62))

(set! *warn-on-reflection* true)

(defn secure-base62-str
  "Generates a cryptographically secure random alphanumeric (base62) string.
  `bits` is an integer representing the number of bits of entropy, this
  should be a multiple of 8."
  [bits]
  {:pre [(pos-int? bits)]}
  (let [bytes (byte-array (Math/ceil (/ bits 8)))]
    (.nextBytes (SecureRandom.) bytes)
    (loop [num (java.math.BigInteger. 1 bytes)
           sb  (StringBuilder.)]
      (if (pos? (.compareTo num BigInteger/ZERO))
        (let [[div rem] (.divideAndRemainder num base62-base)]
          (.append sb (.charAt base62-chars (.intValue rem)))
          (recur div sb))
        (.toString sb)))))
