(ns co.gaiwan.oak.lib.automatic-schema
  "Automatic database schema evolution utilities"
  (:require
   [co.gaiwan.oak.util.log :as log]
   [honey.sql :as sql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as result-set]))

(defn- non-empty-result-set? [rs]
  (reduce
   (fn [acc o] (reduced true))
   false
   (result-set/reducible-result-set rs)))

(defn- table-exists? [^java.sql.Connection db table-name]
  (non-empty-result-set?
   (.getTables (.getMetaData db) nil nil table-name nil)))

(defn- column-exists? [^java.sql.Connection db table-name column-name]
  (non-empty-result-set?
   (.getColumns (.getMetaData db) nil nil table-name column-name)))

(defn- create-table! [db table-name column-defs]
  (log/info :db/creating-table {:table table-name :columns (map first column-defs)})
  (jdbc/execute!
   db
   (sql/format {:create-table table-name
                :with-columns column-defs})))

(defn- add-columns! [db table-name columns]
  (log/info :db/adding-columns {:table table-name :columns (map first columns)})
  (jdbc/execute!
   db
   (sql/format {:alter-table table-name
                :add-column columns})))

(defn evolve-schema!
  "Automatically evolve the database schema based on the provided schema definitions.
  Creates tables that don't exist and adds columns that are missing from existing tables."
  [conn schema-defs]
  (doseq [[table-kw columns] schema-defs]
    (let [columns (conj columns [:created_at :timestamptz [:default :CURRENT_TIMESTAMP]])
          table-name (name table-kw)]
      (if-not (table-exists? conn table-name)
        (create-table! conn table-name columns)
        (when-let [cols (seq
                         (for [col-def columns
                               :let [col (first col-def)]
                               :when (not (column-exists? conn table-name (name col)))]
                           col-def))]
          (add-columns! conn table-name cols))))))