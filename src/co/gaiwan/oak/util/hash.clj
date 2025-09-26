(ns co.gaiwan.oak.util.hash
  "Generic hashing utilities"
  (:require
   [co.gaiwan.oak.util.base64 :as base64])
  (:import
   (java.security MessageDigest)))

(set! *warn-on-reflection* true)

(defn hash-bytes ^bytes [alg o]
  (let [digest (MessageDigest/getInstance ^String alg)
        ^bytes bytes (cond
                       (string? o)
                       (.getBytes ^String o "UTF-8")
                       (bytes? o)
                       o
                       :else
                       (throw (IllegalArgumentException. (str "Unexpected " (class o) ", expected String or byte-array."))))]
    (.update digest bytes)
    (.digest digest)))

(defn sha256-base64url [o]
  (base64/url-encode-no-pad (hash-bytes "SHA-256" o)))

(comment
  (sha256-base64url "test-string")
  ;; => "_-ZfHZj6_t6jUUrclWyK2lmAxsXSVS_WH0hAGu_VwA4"

  )
