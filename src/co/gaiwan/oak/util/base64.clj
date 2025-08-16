(ns co.gaiwan.oak.util.base64
  "Convert between base64 and byte-arrays

  Regular and URL varieties"
  (:import
   (java.util Base64)))

(defn encode ^String [^bytes data]
  (.encodeToString (Base64/getEncoder) data))

(defn encode-no-pad ^String [^bytes data]
  (-> (Base64/getEncoder)
      (.withoutPadding)
      (.encodeToString data)))

(defn decode ^bytes [^String s]
  (.decode (Base64/getDecoder) s))

(defn url-encode ^String [^bytes data]
  (-> (Base64/getUrlEncoder)
      (.encodeToString data)))

(defn url-encode-no-pad ^String [^bytes data]
  (-> (Base64/getUrlEncoder)
      (.withoutPadding)
      (.encodeToString data)))

(defn url-decode ^bytes [^String s]
  (.decode (Base64/getUrlDecoder) s))

(defn encode-str ^bytes [^String s]
  (encode (.getBytes s)))

(defn encode-str-no-pad ^bytes [^String s]
  (encode-no-pad (.getBytes s)))
