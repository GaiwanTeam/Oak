(ns co.gaiwan.oak.system.http
  "HTTP server component"
  (:require
   [co.gaiwan.oak.util.log :as log]
   [reitit.ring :as retit-ring]
   [s-exp.hirundo :as hirundo]))

(defn start [config]
  (log/info :http/starting {:port (:port config)})
  {:server
   (hirundo/start!
    {:http-handler
     (retit-ring/ring-handler (:router config))
     :port (:port config)})})

(def component
  {:start start
   :stop (fn [o] (prn o) (hirundo/stop! (:server o)))})
