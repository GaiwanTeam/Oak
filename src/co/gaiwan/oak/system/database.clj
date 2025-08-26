(ns co.gaiwan.oak.system.database
  "PostgreSQL connection pool + JSONB setup"
  (:require
   [co.gaiwan.oak.app.schema :as schema]
   [co.gaiwan.oak.lib.automatic-schema :as automatic-schema]
   [co.gaiwan.oak.lib.pg-jsonb :as pg-jsonb]
   [co.gaiwan.oak.util.log :as log]
   [honey.sql :as sql]
   [next.jdbc :as jdbc])
  (:import
   (com.zaxxer.hikari HikariConfig HikariDataSource)
   (java.sql Connection)))

(set! *warn-on-reflection* true)

(defn- set-hikari-option!
  "Sets a specific option on a HikariConfig object. This helper function
  avoids reflection by explicitly mapping keywords to setter methods."
  [^HikariConfig config key value]
  (case key
    :catalog (.setCatalog config (str value))
    :connection-init-sql (.setConnectionInitSql config (str value))
    :connection-test-query (.setConnectionTestQuery config (str value))
    :connection-timeout (.setConnectionTimeout config (long value))
    :idle-timeout (.setIdleTimeout config (long value))
    :initialization-fail-timeout (.setInitializationFailTimeout config (long value))
    :keepalive-time (.setKeepaliveTime config (long value))
    :leak-detection-threshold (.setLeakDetectionThreshold config (long value))
    :max-lifetime (.setMaxLifetime config (long value))
    :maximum-pool-size (.setMaximumPoolSize config (int value))
    :minimum-idle (.setMinimumIdle config (int value))
    :password (.setPassword config (str value))
    :pool-name (.setPoolName config (str value))
    :read-only (.setReadOnly config (boolean value))
    :register-mbeans (.setRegisterMbeans config (boolean value))
    :schema (.setSchema config (str value))
    :username (.setUsername config (str value))
    :validation-timeout (.setValidationTimeout config (long value))
    (throw (ex-info (str "Unrecognized Hikari config key " key) {:key key :value value}))))

(defn hikari-data-source
  "Creates and configures a HikariCP DataSource from a map of options.

  Required key:
    :url      - The PostgreSQL JDBC URL (e.g., \"jdbc:postgresql://host:port/db\")

  Recommended keys:
    :username - Database username
     :password - Database password

  Common HikariCP pool tuning keys (as kebab-case keywords):
    :auto-commit        - boolean
    :connection-timeout - long (milliseconds)
    :idle-timeout       - long (milliseconds)
    :max-lifetime       - long (milliseconds)
    :minimum-idle       - int
    :maximum-pool-size  - int
    :pool-name          - string
    :read-only          - boolean
    :validation-timeout - long (milliseconds)

  See the internal `set-hikari-option!` function for a full list of supported keys."
  [opts]
  (let [config (HikariConfig.)]
    (assert (:url opts))
    (.setJdbcUrl config (:url opts))
    (doseq [[k v] opts
            :when (not (#{:url} k))]
      (set-hikari-option! config k v))
    (HikariDataSource. config)))

(def component
  {:start (fn [{:keys [config]}]
            (let [ds (hikari-data-source config)]
              (with-open [conn (jdbc/get-connection ds)]
                (automatic-schema/evolve-schema! conn schema/schema)
                (run! #(jdbc/execute! conn (sql/format %)) schema/indices))
              (assoc config
                     :data-source ds
                     :http/request-filter (fn [req] (assoc req :db ds)))))
   :stop #(when-let [^HikariDataSource ds (:data-source %)]
            (.close ds))})
