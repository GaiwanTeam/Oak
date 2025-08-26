(ns co.gaiwan.oak.test-harness
  (:require
   [clojure.string :as str]
   [co.gaiwan.oak.app.schema :as schema]
   [co.gaiwan.oak.lib.automatic-schema :as automatic-schema]
   [co.gaiwan.oak.lib.pg-jsonb :as pg-jsonb]
   [honey.sql :as sql]
   [lambdaisland.config :as config]
   [next.jdbc :as jdbc]))

(defonce config
  (config/create {:env :test :prefix "oak"}))

(def ^:dynamic *db* nil)

(defn db-config
  "Get database config in next.jdbc format"
  []
  (let [cfg (config/get config :db/config)]
    {:jdbcUrl (:url cfg)
     :user (:username cfg)
     :password (:password cfg)}))

(defn db-admin-config
  "Get admin database config in next.jdbc format"
  []
  (let [cfg (config/get config :db/admin-config)]
    {:jdbcUrl (:url cfg)
     :user (:username cfg)
     :password (:password cfg)}))

(defn- get-db-name
  "Extract database name from JDBC URL"
  [url]
  (second (re-find #"jdbc:postgresql://[^/]+/([^?]+)" url)))

(defn- create-test-database!
  "Create the test database if it doesn't exist and make the regular user the owner"
  [db-config]
  (let [orig-cfg (config/get config :db/config)
        db-name (get-db-name (:url orig-cfg))
        admin-cfg (db-admin-config)
        db-user (:user db-config)]
    (when db-name
      (with-open [conn (jdbc/get-connection admin-cfg)]
        (let [db-exists? (jdbc/execute! conn
                                        ["SELECT 1 FROM pg_database WHERE datname = ?" db-name])]
          (when (empty? db-exists?)
            (jdbc/execute! conn [(str "CREATE DATABASE " db-name " OWNER " db-user)])))))))

(defn- drop-test-database!
  "Drop the test database"
  [db-config]
  (let [orig-cfg (config/get config :db/config)
        db-name (get-db-name (:url orig-cfg))
        admin-cfg (db-admin-config)]
    (when db-name
      (with-open [conn (jdbc/get-connection admin-cfg)]
        (jdbc/execute! conn [(str "DROP DATABASE IF EXISTS " db-name)])))))

(defn- setup-test-schema!
  "Set up the test database schema"
  [db-config]
  (with-open [conn (jdbc/get-connection db-config)]
    (automatic-schema/evolve-schema! conn schema/schema)
    (run! #(jdbc/execute! conn (sql/format %)) schema/indices)))

(defn with-test-database
  "Test fixture that creates a test database, sets up schema, and cleans up afterwards.
  Usage: (use-fixtures :once (with-test-database))"
  [f]
  (let [config (db-config)]
    (create-test-database! config)
    (setup-test-schema! config)
    (binding [*db* config]
      (f))
    (drop-test-database! config)))

(comment
  (db-config)
  ;; => {:url "jdbc:postgresql://localhost:5432/oak_test",
  ;;     :username "postgres",
  ;;     :password "oak"}
  (db-admin-config)
  ;; => {:url "jdbc:postgresql://localhost:5432/postgres",
  ;;     :username "postgres",
  ;;     :password "oak"}
  )
