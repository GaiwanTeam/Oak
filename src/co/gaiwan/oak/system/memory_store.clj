(ns co.gaiwan.oak.system.memory-store
  "Memory-based session store, used for testing"
  (:require
   [ring.middleware.session.memory :as ring-sess-mem]))

(defn component [_]
  (ring-sess-mem/memory-store))
