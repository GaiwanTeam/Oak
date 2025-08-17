(ns co.gaiwan.oak.app.config
  (:refer-clojure :exclude [get])
  (:require
   [clojure.java.io :as io]
   [co.gaiwan.oak.util.log :as log]
   [lambdaisland.config :as config]
   [lambdaisland.config.cli :as config-cli]
   [lambdaisland.makina.app :as app]))

(def prefix "oak")

(defonce cli-opts (atom {}))

(defonce config
  (config-cli/add-provider
   (config/create {:prefix prefix})
   cli-opts))

(def get (partial config/get config))
(def source (partial config/source config))
(def sources (partial config/sources config))
(def entries (partial config/entries config))
(def reload! (partial config/reload! config))

(def system
  (app/create
   {:prefix prefix
    :ns-prefix "co.gaiwan.oak.system"
    :data-readers {'config get}}))

(def load! (partial app/load! system))
(def start! (partial app/start! system))
(def stop! (partial app/stop! system))
(def refresh (partial app/refresh `system))
(def refresh-all (partial app/refresh-all `system))

(comment
  system
  (load!)
  (start!)
  (stop!)
  (refresh))

(defn reload-full! []
  (log/info :message "Configuration changed, reloading config and restarting app")
  (reload!)
  (refresh))

(when-let [watch! (try (requiring-resolve 'lambdaisland.launchpad.watcher/watch!) (catch Exception _))]
  (let [local-config (io/file "config.local.edn")]
    (watch!
     (cond-> {(.getCanonicalPath (io/file (.toURI (io/resource (str prefix "/config.edn"))))) (fn [_] (reload-full!))
              (.getCanonicalPath (io/file (.toURI (io/resource (str prefix "/dev.edn"))))) (fn [_] (reload-full!))}
       (.exists local-config)
       (assoc (.getCanonicalPath local-config) (fn [_] (reload-full!)))))))
