(ns co.gaiwan.oak.test-harness
  (:require
   [co.gaiwan.oak.app.config :as oak-config]
   [co.gaiwan.oak.domain.jwk :as jwk]
   [co.gaiwan.oak.lib.db :as db]
   [lambdaisland.config :as config]
   [lambdaisland.makina.system :as sys]
   [lambdaisland.makina.test :as makina-test]
   [next.jdbc :as jdbc]))

(require 'co.gaiwan.oak.lib.pg-jsonb)

(defonce config
  (config/create {:env :test :prefix oak-config/prefix}))

(def get-config (partial config/get config))

(def makina-opts
  {:prefix oak-config/prefix
   :ns-prefix oak-config/ns-prefix
   :data-readers {'config get-config}
   :handlers {}})

;; Test helpers
(defn component [k]
  (sys/component (:makina/system makina-test/*sys*) k))

(defn handler [] (component :system/handler))
(defn db [] (:data-source (component :system/database)))
(defn session-store [] (component :system/session-store))

(defn ensure-jwk! []
  (when-not (jwk/default-key (db))
    (jwk/create! (db) {"kty" "OKP"})))

;; Fixtures
(def with-system (makina-test/make-fixture-factory makina-opts))

(defn with-auto-rollback []
  (fn [t]
    (try
      (t)
      (finally
        (with-open [conn (jdbc/get-connection (db))]
          (doseq [tbl (db/table-names conn)]
            (jdbc/execute! (db) [(str "TRUNCATE " tbl " CASCADE")])))))))

(defn with-ensure-jwk []
  (fn [t]
    (ensure-jwk!)
    (t)))

(comment
  ;; for REPL use, permanently sets *sys*
  (makina-test/start! makina-opts [:system/handler])
  makina-test/*sys*

  (doseq [tbl (#'co.gaiwan.oak.lib.automatic-schema/table-names (jdbc/get-connection (db)))]
    (jdbc/execute! (db) [(str "TRUNCATE " tbl " CASCADE")]))

  (for [tbl (#'co.gaiwan.oak.lib.automatic-schema/table-names (jdbc/get-connection (db)))]
    (jdbc/execute! (db) [(str "SELECT count(*) FROM " tbl )]))
  )
