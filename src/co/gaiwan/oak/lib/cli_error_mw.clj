(ns co.gaiwan.oak.lib.cli-error-mw
  "lambdaisland/cli middleware for convenient and pretty error handling"
  (:require
   [clj-commons.format.exceptions :as pretty]
   [clojure.pprint :as pprint]))

(defn print-error [opts e]
  (if (:show-trace opts)
    (pretty/print-exception e)
    (do
      (println "Error:" (ex-message e))
      (when-let [d (ex-data e)]
        (pprint/pprint d))
      (println "Use --show-trace to see the full error trace."))))

(defn wrap-error [handler]
  (fn [opts]
    (try
      (handler opts)
      (catch clojure.lang.ExceptionInfo e
        (let [d (ex-data e)]
          (if (= :lambdaisland.cli/parse-error (:type d))
            (println (ex-message e))
            (print-error opts e))))
      (catch Throwable t
        (print-error opts t)))))
