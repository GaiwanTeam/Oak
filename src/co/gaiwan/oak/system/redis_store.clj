(ns co.gaiwan.oak.system.redis-store
  "Redis component"
  (:require
   [ring.redis.session :as ring-redis]
   [taoensso.carmine :as carmine]))

(defn component [opts]
  (ring-redis/redis-store
   {:pool (carmine/connection-pool {})
    :spec opts}))
