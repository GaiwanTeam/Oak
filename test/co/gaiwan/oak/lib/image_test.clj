(ns co.gaiwan.oak.lib.image-test
  (:require
   [clojure.test :refer :all]
   [co.gaiwan.oak.lib.image :as image]
   [co.gaiwan.oak.test-fixture-jetty :refer [with-jetty-server serve-file]]
   [clojure.java.io :as io])
  (:import
   [java.awt.image BufferedImage]))

(defn make-test-image
  ([] (make-test-image 100 100))
  ([w h]
   (BufferedImage. w h BufferedImage/TYPE_INT_RGB)))

(deftest buffered-image->bytes
  (let [img (make-test-image)
        bytes (image/buffered-image->bytes img "png")]
    (is (bytes? bytes))
    (is (> (count bytes) 0))))

(deftest fetch-image-integration
  (with-jetty-server
    {:port 18089
     :routes {"/img.png" (serve-file "test/resources/oak/assets/test-image.png")}}
    (let [img (image/fetch-image "http://localhost:18089/img.png")]
      (is (instance? java.awt.image.BufferedImage img)))))
