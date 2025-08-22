(ns repl-session.system
  (:require [co.gaiwan.oak.app.config :as config]))


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
