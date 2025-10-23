(ns co.gaiwan.oak.apis.assets
  "Static assets loaded from resources"
  (:require
   [clojure.java.io :as io]
   [lambdaisland.ornament :as o]))

(defn GET-styles-css
  "The ornament/global styles, either pre-rendered, or directly from ornament"
  [_]
  {:status 200
   :body (try
           (slurp (io/resource "oak/styles.css"))
           (catch Exception e
             (require 'co.gaiwan.oak.html.styles)
             (o/defined-styles)))
   :headers {"Content-Type" "text/css;charset=utf-8"}})

(defn GET-favicon-ico
  "We got a basic favicon.ico for now, not all the fancy apple head tags"
  [_]
  {:status 200
   :body (io/input-stream (io/resource "oak/favicon.ico"))
   :headers {"Content-Type" "image/x-icon"}})

(defn component [_]
  {:routes
   ["" {}
    ["/styles.css" {:get #'GET-styles-css}]
    ["/favicon.ico" {:get #'GET-favicon-ico}]]})
