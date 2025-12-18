(ns co.gaiwan.oak.lib.image
  "Image manipulation utilities"
  (:require
   [hato.client :as hc]
   [co.gaiwan.oak.util.log :as log]
   [clojure.java.io :as io])
  (:import
   (java.io ByteArrayInputStream ByteArrayOutputStream)
   (java.awt.image BufferedImage)
   (javax.imageio ImageIO)
   (java.awt Graphics2D)
   (java.awt.geom AffineTransform)
   (java.awt RenderingHints)))

(set! *warn-on-reflection* true)

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
  [^BufferedImage image ^String format-name file-path]
  (try
    (let [output-file (io/file file-path)]
      (ImageIO/write image format-name output-file))
    (catch Exception e
      (log/error e "Failed to save image to" file-path)
      false)))

(defn buffered-image->bytes
  [^BufferedImage image ^String format-name]
  (when image
    (with-open [baos (ByteArrayOutputStream.)]
      (ImageIO/write image format-name baos)
      (.toByteArray baos))))

(defn resize-to-height
  "Resize a BufferedImage to the specified height while maintaining aspect ratio"
  [^BufferedImage image target-height]
  (when image
    (let [original-width (.getWidth image)
          original-height (.getHeight image)
          aspect-ratio (/ (double original-width) (double original-height))
          new-width (int (* target-height aspect-ratio))]
      (if (>= original-height target-height)
        (let [scaled-image (BufferedImage. new-width target-height (.getType image))
              g2d (.createGraphics scaled-image)]
          (.drawImage g2d image
                      0 0 new-width target-height
                      0 0 original-width original-height
                      nil)
          (.dispose g2d)
          scaled-image)
        image))))

(defn fetch-and-resize [uri]
  (as-> uri x
    (fetch-image x)
    (resize-to-height x 32)
    (buffered-image->bytes x "png")))

(comment
  (def test-uri "https://gaiwan.co/content/images/2024/01/Gaiwan-logo-transparent-bg.png")
  (def image-result (fetch-image test-uri))
  (def resized-img (resize-to-height image-result 32))
  (fetch-and-resize test-uri)
  (save-image-to-file resized-img "png" "./gaiwan-bg.png"))
