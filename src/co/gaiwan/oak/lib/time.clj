(ns co.gaiwan.oak.lib.time
  "Handle time format"
  (:require
   [co.gaiwan.oak.app.config :as config]
   [java-time :as t])
  (:import (java.util Locale)))

(defn format-date [inst]
  (let [zone (t/zone-id (config/get :time/zone-id))
        zdt (t/zoned-date-time inst zone)]
    (t/format (.withLocale (t/formatter "MMM d, yyyy") Locale/ENGLISH) zdt)))

(comment
  (def my-inst #inst "2023-08-05T10:30:00.000-00:00")
  (prn (format-date my-inst)))
