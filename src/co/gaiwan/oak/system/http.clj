(ns co.gaiwan.oak.system.http
  "HTTP server component"
  (:require
   [co.gaiwan.oak.util.log :as log]
   [reitit.ring :as reitit-ring]
   [ring.adapter.jetty :as jetty]))

(defn start [config]
  (log/info :http/starting {:port (:port config)})
  {:server
   (jetty/run-jetty
    (:handler config)
    {:host  (:host config)
     :port  (:port config)
     :join? false})})

(def component
  {:start start
   :stop (fn [o] (prn o) (.stop (:server o)))})

(comment
  (user/restart! :system/http))
