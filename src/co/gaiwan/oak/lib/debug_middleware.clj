(ns co.gaiwan.oak.lib.debug-middleware
  (:require
   [co.gaiwan.oak.util.log :as log]))

(defn wrap-log-response [h]
  (fn [req]
    (try
      (let [res (h req)]
        (log/debug :http/response res
                   ;; :http/request req
                   )
        res)
      (catch Throwable t
        (log/debug :http/response :threw :exception t)
        (throw t)))))
