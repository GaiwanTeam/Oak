(ns user)

(defn go []
  ((requiring-resolve 'co.gaiwan.oak.app.config/start!)))
