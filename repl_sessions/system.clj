(ns repl-session.system
  (:require
   [co.gaiwan.oak.app.config :as config]))


(lambdaisland.makina.system/error
 (:makina/system @config/system))
(lambdaisland.makina.app/error config/system)
(some :makina/error
      (vals
       (:makina/system @config/system)))
(config/start!)
(config/stop!)

(config/error)

(config/print-table)
(require '[hato.client :as http])

(try
  (let [response (http/request {:url "http://localhost:4800/.well-known/scim-configuration"
                                :method :get
                                :headers {"Accept" "application/scim+json"}
                                :throw-exceptions false})]
    (:status response))
  (catch Exception e
    (.getMessage e)))
