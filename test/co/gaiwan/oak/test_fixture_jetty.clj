(ns co.gaiwan.oak.test-fixture-jetty
  (:require [ring.adapter.jetty :as jetty]
            [clojure.java.io :as io]
            [ring.util.response :as resp]))

;; ---------------------------------------------
;; Handlers / helpers
;; ---------------------------------------------

(defn serve-file
  "Serve a file from disk. Content type auto-detected by Ring."
  [file-path]
  (fn [_req]
    (-> file-path
        resp/file-response)))

(defn route-dispatcher
  "Given a map of path -> handler, return a Ring handler.
   Example:
     {\"/a\" (fn [r] ...)
      \"/img.png\" (serve-file ...)}"
  [routes]
  (fn [req]
    (let [uri (:uri req)
          handler (get routes uri)]
      (if handler
        (handler req)
        {:status 404 :body "Not Found"}))))

;; ---------------------------------------------
;; Jetty fixture
;; ---------------------------------------------

(defn with-jetty-server*
  "Start an embedded Jetty server, run f, then stop the server.
   opts:
   {:port <port>
    :routes {\"/path\" handler ...}}"
  [{:keys [port routes]} f]
  (let [handler (route-dispatcher routes)
        server  (jetty/run-jetty handler {:port port :join? false})]
    (try
      (f)
      (finally
        (.stop server)))))

(defmacro with-jetty-server
  "Usage:
    (with-jetty-server {:port 18089
                        :routes {\"/img.png\" (serve-file \"test/resources/test.png\")}}
      (is ...))"
  [opts & body]
  `(with-jetty-server* ~opts (fn [] ~@body)))
