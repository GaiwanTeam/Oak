(ns co.gaiwan.oak.util.uuid
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.util.bigint :as bigint])
  (:import
   (java.util UUID)))

(defn uuid->biginteger
  "Convert to bigint, retaining the sort order of a uuid/v7"
  [^UUID uuid]
  (let [msb (.getMostSignificantBits uuid)
        lsb (.getLeastSignificantBits uuid)
        bytes (byte-array 16)]
    (dotimes [i 8]
      (aset-byte bytes (- 15 i) (unchecked-byte (bit-shift-right lsb (* 8 i))))
      (aset-byte bytes (- 7 i) (unchecked-byte (bit-shift-right msb (* 8 i)))))
    (BigInteger. 1 bytes)))

(defn uuid->base62
  "Compact representation of a uuid, as a 22 char alphanumeric string

  Useful in URLs, or other places where we don't have a native UUID type"
  [^UUID uuid]
  (bigint/bigint->base62 (uuid->biginteger uuid)))

(defn random-compacted-uuid []
  (uuid->base62 (uuid/v7)))
