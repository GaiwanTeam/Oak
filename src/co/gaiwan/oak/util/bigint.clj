(ns co.gaiwan.oak.util.bigint
  "Unopinioted BigInteger helpers"
  (:import
   (java.math BigInteger)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(def ^:private base62-chars "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")
(def ^:private base62-base (BigInteger/valueOf 62))

(defn bigint->base62 [^BigInteger num]
  (loop [num num
         sb  (StringBuilder.)]
    (if (pos? (.compareTo num BigInteger/ZERO))
      (let [[div rem] (.divideAndRemainder num base62-base)]
        (.append sb (.charAt ^String base62-chars (.intValue ^BigInteger rem)))
        (recur div sb))
      (.toString (.reverse sb)))))
