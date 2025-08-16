(ns co.gaiwan.oak.util.bigint-test
  (:require
   [co.gaiwan.oak.util.bigint :as bigint]
   [clojure.test :refer :all]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn bytes->bits
  "Show the bit pattern of a byte array"
  [^bytes ba]
  (map #(Integer/toBinaryString (bit-and % 0xFF)) ba))

(deftest unsigned-bytes-test
  (is (= ["0"] (bytes->bits (bigint/unsigned-bytes (BigInteger. "0")))))
  (is (= ["1"] (bytes->bits (bigint/unsigned-bytes (BigInteger. "1")))))
  (is (= ["10"] (bytes->bits (bigint/unsigned-bytes (BigInteger. "2")))))
  (is (= ["101"] (bytes->bits (bigint/unsigned-bytes (BigInteger. "5")))))
  (is (= ["1111111"] (bytes->bits (bigint/unsigned-bytes (BigInteger. "127")))))
  ;; Gets a leading zero from Java, has been removed
  (is (= ["10000000"] (bytes->bits (bigint/unsigned-bytes (BigInteger. "128")))))
  (is (= ["11111111"] (bytes->bits (bigint/unsigned-bytes (BigInteger. "255")))))
  (is (= ["1" "0"] (bytes->bits (bigint/unsigned-bytes (BigInteger. "256"))))))
