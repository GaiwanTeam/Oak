(ns co.gaiwan.oak.http
  (:require
   [s-exp.hirundo :as hirundo]))

(defn start [config]
  (hirundo/start!
   {:http-handler (fn [{:as request :keys [body headers]}]
                    {:status 200
                     :body "Hello world"
                     :headers {"Something" "Interesting"}})
    :port (:port config)}))

(def component
  {:start start
   :stop hirundo/stop!})
