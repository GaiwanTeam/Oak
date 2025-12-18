(ns co.gaiwan.oak.test-harness
  "Fixtures to set up a test system, set up the database, etc."
  (:require
   [co.gaiwan.oak.app.config :as oak-config]
   [co.gaiwan.oak.domain.jwk :as jwk]
   [co.gaiwan.oak.lib.db :as db]
   [lambdaisland.uri :as uri]
   [lambdaisland.config :as config]
   [lambdaisland.makina.system :as sys]
   [lambdaisland.makina.test :as makina-test]
   [next.jdbc :as jdbc]
   [co.gaiwan.oak.system.database :as system-db]))

(require 'co.gaiwan.oak.lib.pg-jsonb)

(defonce config
  (config/create {:env :test :prefix oak-config/prefix}))

(def get-config (partial config/get config))

(defn- create-test-database!
  "Create the test database if it doesn't exist and make the regular user the owner"
  []
  (let [jdbc-url  (uri/uri (config/get config :db/url))
        pg-url    (uri/uri (:path jdbc-url))
        db-name   (subs (:path pg-url) 1)
        db-user   (config/get config :db/username)
        db-pass   (config/get config :db/password)
        admin-url (str "jdbc:" (-> (assoc pg-url :path "/postgres")
                                   (uri/assoc-query :user db-user :password db-pass)))]
    (when db-name
      (with-open [conn (jdbc/get-connection {:jdbcUrl admin-url})]
        (let [db-exists? (jdbc/execute! conn
                                        ["SELECT 1 FROM pg_database WHERE datname = ?" db-name])]
          (when (empty? db-exists?)
            (jdbc/execute! conn [(str "CREATE DATABASE " db-name " OWNER " db-user)])))))))

(defn start-db [config]
  (create-test-database!)
  ((:start system-db/component) config))

(def makina-opts
  {:prefix oak-config/prefix
   :ns-prefix oak-config/ns-prefix
   :data-readers {'config get-config}
   :handlers
   {:system/database
    {:start start-db, :stop (:stop system-db/component)}}})

;; Test helpers

(def component makina-test/component)
(defn handler [] (component :system/handler))
(defn db [] (:data-source (component :system/database)))
(defn session-store [] (component :system/session-store))

(defn ensure-jwk! []
  (when-not (jwk/default-key (db))
    (jwk/create! (db) {"kty" "OKP"})))

;; Fixtures

(def with-system (makina-test/make-fixture-fn makina-opts))

(defn with-auto-truncate
  "Empty all tables at the end of each test"
  []
  (fn [t]
    (try
      (t)
      (finally
        (with-open [conn (jdbc/get-connection (db))]
          (doseq [tbl (db/table-names conn)]
            (jdbc/execute! (db) [(str "TRUNCATE " tbl " CASCADE")])))))))

(defn with-ensure-jwk
  "Make sure a JWK has been generated and inserted into the database, since any
  JWT handling expects at least one JWK to be defined."
  []
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

  (lambdaisland.makina.app/load* (lambdaisland.makina.app/create* makina-opts))
  )
