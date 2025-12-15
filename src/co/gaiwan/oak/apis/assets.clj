(ns co.gaiwan.oak.apis.assets
  "Static assets loaded from resources

  Currently we're hard-coding specific asset routes. This isn't really scalable,
  but we shouldn't need many of them, and it means we don't end up serving
  things off the classpath or filesystem which we didn't intend to serve."
  (:require
   [clojure.java.io :as io]
   [lambdaisland.ornament :as o]))

(defn GET-styles-css
  "The ornament/global styles, either pre-rendered, or directly from ornament"
  [_]
  {:status 200
   :body (try
           (slurp (io/resource "oak/assets/styles.css"))
           (catch Exception e
             (require 'co.gaiwan.oak.html.styles)
             (o/defined-styles)))
   :headers {"Content-Type" "text/css;charset=utf-8"}})

(defn GET-favicon-ico
  "We got a basic favicon.ico for now, not all the fancy apple head tags"
  [_]
  {:status 200
   :body (io/input-stream (io/resource "oak/assets/favicon.ico"))
   :headers {"Content-Type" "image/x-icon"}})

(defn GET-oak-js
  "Sprinkles of JS in a single file"
  [_]
  {:status 200
   :body (io/input-stream (io/resource "oak/assets/oak.js"))
   :headers {"Content-Type" "application/javascript"}})

(defn component [_]
  {:routes
   ["" {:auth-claim :public}
    ["/styles.css" {:get #'GET-styles-css}]
    ["/favicon.ico" {:get #'GET-favicon-ico}]
    ["/scripts/oak.js" {:get #'GET-oak-js}]]})
