(ns co.gaiwan.oak.util.hash
  "Generic hashing utilities"
  (:require
   [co.gaiwan.oak.util.base64 :as base64])
  (:import
   (java.security MessageDigest)))

(defn sha256-base64url [string]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes string "UTF-8"))
    (base64/url-encode-no-pad (.digest digest))))

(comment
  (sha256-base64url "test-string")
  ;; => "_-ZfHZj6_t6jUUrclWyK2lmAxsXSVS_WH0hAGu_VwA4"

  )
