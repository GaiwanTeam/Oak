(ns co.gaiwan.oak.util.routing
  "Helpers related to the reitit router/routing"
  (:require
   [reitit.core :as reitit]))

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
