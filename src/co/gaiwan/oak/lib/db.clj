(ns co.gaiwan.oak.lib.db
  "Convenience functions for next.jdbc/honeysql

  Default to returning kebab-cased qualified maps.
  "
  (:require
   [honey.sql :as honey]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [next.jdbc.sql :as sql]))

(defn execute! [ds qry]
  (jdbc/execute!
   ds
   qry
   {:builder-fn rs/as-kebab-maps}))

(defn execute-honey! [ds qry]
  (execute!
   ds
   (honey/format qry)))

(defn insert! [ds table key-map]
  (sql/insert! ds table key-map
               {:builder-fn rs/as-kebab-maps}))
