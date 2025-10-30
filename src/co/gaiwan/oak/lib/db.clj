(ns co.gaiwan.oak.lib.db
  "Convenience functions for next.jdbc/honeysql

  Default to returning kebab-cased qualified maps.
  "
  (:require
   [clojure.walk :as walk]
   [honey.sql :as honey]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [next.jdbc.sql :as sql])
  (:import
   (java.time Instant)))

(defn munge-response [o]
  (cond
    (instance? java.sql.Timestamp o)
    (.toInstant ^java.sql.Timestamp o)

    :else
    o))

(defn execute! [ds qry]
  (walk/postwalk
   munge-response
   (jdbc/execute!
    ds
    qry
    {:builder-fn rs/as-kebab-maps})))

(defn execute-honey! [ds qry]
  (execute!
   ds
   (honey/format qry)))

(defn insert! [ds table key-map]
  (sql/insert! ds table key-map
               {:builder-fn rs/as-kebab-maps}))

(defn update!
  "Perform a UPDATE query, sets `updated_at`"
  [ds {:keys [update set where]}]
  (execute-honey!
   ds {:update update
       :set (assoc set :updated_at (Instant/now))
       :where where}))

(defmacro with-transaction
  {:doc (:doc (meta #'jdbc/with-transaction))}
  [[sym transactable opts] & body]
  `(jdbc/with-transaction [~sym ~transactable ~opts] ~@body))
