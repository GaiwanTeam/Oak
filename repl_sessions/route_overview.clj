(ns route-overview
  (:require
   [clojure.string :as str]
   [co.gaiwan.oak.app.config :as config]
   [reitit.core :as r]))


(defn print-routes-overview [router]
  (let [routes (r/routes router)]
    (doseq [[path route-data] routes
            [method handler] route-data]
      (when (#{:get :put :post :delete} method)
        (let [method-name (str/upper-case (name method))]
          (println
           (format "%-7s %s"
                   method-name
                   path)))))))
(print-routes-overview
 (config/component :system/router))
