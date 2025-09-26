(ns co.gaiwan.oak.util.routing
  "Helpers related to the reitit router/routing"
  (:require
   [reitit.core :as reitit]))

(defn base-url [req]
  (let [{:keys [headers authority scheme host]} req
        host (or (get headers "host") ;; HTTP 1.1
                 authority)] ;; HTTP 2.0
    (str (name scheme) "://" host)))

(defn path-for
  ([req name]
   (reitit/match->path
    (reitit/match-by-name!
     (:reitit.core/router req)
     name)))
  ([req name params]
   (reitit/match->path
    (reitit/match-by-name!
     (:reitit.core/router req)
     name params))))

(defn url-for
  ([req name]
   (str (base-url req) (path-for req name)))
  ([req name params]
   (str (base-url req) (path-for req name params))))
