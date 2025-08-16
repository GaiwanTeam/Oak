(ns co.gaiwan.oak.util.bigint
  "Unopinioted BigInteger helpers"
  (:require
   [co.gaiwan.oak.util.base64 :as base64])
  (:import
   (java.util Arrays)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn unsigned-bytes
  "Converts a BigInteger to a minimal unsigned big-endian byte array."
  ^bytes [^BigInteger n]
  (let [bs (.toByteArray n)]
    ;; Strip leading zero sign byte that java may add
    (if (and (> (alength bs) 1) (zero? (aget bs 0)))
      (Arrays/copyOfRange bs 1 (alength bs))
      bs)))

(defn b64url
  "Converts a BigInteger to a Base64URL string."
  [^BigInteger n]
  (-> n to-unsigned-byte-array base64/url-encode-no-pad))

(defn b64url-padded
  "Converts a BigInteger to a Base64URL string, padded to a specific length.
  Required for EC key coordinates."
  [^BigInteger n ^long len]
  (let [^bytes unsigned-bytes (to-unsigned-byte-array n)
        pad-len (- len (alength unsigned-bytes))]
    (if (pos? pad-len)
      (let [padded-bytes (byte-array len)]
        (System/arraycopy unsigned-bytes 0 padded-bytes pad-len (alength unsigned-bytes))
        (base64/url-encode-no-pad padded-bytes))
      (base64/url-encode-no-pad unsigned-bytes))))

(defn unsigned-from-b64
  "Decodes a Base64URL string into a positive BigInteger."
  ^BigInteger [^String s]
  (BigInteger. 1 (base64/url-decode s)))
