(ns co.gaiwan.oak.system.http
  "HTTP server component"
  (:require
   [co.gaiwan.oak.util.log :as log]
   [reitit.ring :as reitit-ring]
   [s-exp.hirundo :as hirundo]))

(defn start [config]
  (log/info :http/starting {:port (:port config)})
  {:server
   (hirundo/start!
    {:http-handler (reitit-ring/ring-handler (:router config)
                                             (reitit-ring/create-default-handler))
     :host         (:host config)
     :port         (:port config)})})

(def component
  {:start start
   :stop (fn [o] (prn o) (hirundo/stop! (:server o)))})

(comment
  (user/restart! :system/http))
