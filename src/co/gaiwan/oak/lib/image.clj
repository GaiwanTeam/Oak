(ns co.gaiwan.oak.lib.image
  (:require [hato.client :as hc]
            [co.gaiwan.oak.util.log :as log]
            [clojure.java.io :as io]
            [image-resizer.core :as img-resize])
  (:import [java.io ByteArrayInputStream]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO]))

(defn fetch-image
  "Fetch an image from remote URI"
  [uri]
  (try
    (let [response (hc/get uri {:as :byte-array})
          image-bytes (:body response)]
      (ImageIO/read (ByteArrayInputStream. image-bytes)))
    (catch Exception e
      (log/error e "Failed to fetch image from" uri)
      nil)))

(defn save-image-to-file
  "Saves the BufferedImage to the specified file path, designating the image format (e.g., 'png' or 'jpg'). Returns true/false indicating whether the write operation was successful."
  [^BufferedImage image format-name file-path]
  (try
    (let [output-file (io/file file-path)]
      (ImageIO/write image format-name output-file))
    (catch Exception e
      (log/error e "Failed to save image to" file-path)
      false)))

(defn fetch-and-resize [uri])

(comment
  (def test-uri "https://gaiwan.co/content/images/2024/01/Gaiwan-logo-transparent-bg.png")
  (def image-result (fetch-image test-uri))
  (def resized-img (img-resize/resize-to-height image-result 32))
  (save-image-to-file resized-img "png" "./gaiwan-bg.png"))
