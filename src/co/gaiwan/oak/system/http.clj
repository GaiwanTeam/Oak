(ns co.gaiwan.oak.system.http
  (:require
   [reitit.ring :as retit-ring]
   [s-exp.hirundo :as hirundo]))

(defn start [config]
  (prn config)
  (println "Starting http at" (:port config)
           "with" (class (:router config)))
  {:server
   (hirundo/start!
    {:http-handler
     (retit-ring/ring-handler (:router config))
     :port (:port config)})})

(def component
  {:start start
   :stop (fn [o] (prn o) (hirundo/stop! (:server o)))})
