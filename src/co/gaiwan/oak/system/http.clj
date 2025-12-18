(ns co.gaiwan.oak.system.http
  "HTTP server component"
  (:require
   [co.gaiwan.oak.util.log :as log]
   [reitit.ring :as reitit-ring]
   [ring.adapter.jetty :as jetty]))

(defn start [{:keys [port host handler]}]
  (log/info :http/starting {:port port})
  {:server
   (jetty/run-jetty
    handler
    {:host  host
     :port  (if (string? port) (parse-long port) port)
     :join? false})})

(def component
  {:start start
   :stop (fn [o] (.stop (:server o)))})

(comment
  (user/restart! :system/http))
