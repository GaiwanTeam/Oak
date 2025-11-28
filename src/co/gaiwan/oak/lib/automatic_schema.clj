(ns co.gaiwan.oak.lib.automatic-schema
  "Automatic database schema evolution utilities"
  (:require
   [co.gaiwan.oak.util.log :as log]
   [co.gaiwan.oak.lib.db :as db]
   [honey.sql :as sql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as result-set]))

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
  (let [table-exists? (db/table-names conn)]
    (doseq [[table-kw columns] schema-defs]
      (let [columns (conj columns
                          [:created_at :timestamptz [:default :CURRENT_TIMESTAMP]]
                          [:updated_at :timestamptz [:default :CURRENT_TIMESTAMP]])
            table-name (name table-kw)]
        (if-not (table-exists? table-name)
          (create-table! conn table-name columns)
          (let [col-exists? (db/table-cols conn table-name)]
            (when-let [cols (seq
                             (for [col-def columns
                                   :let [col (first col-def)]
                                   :when (not (col-exists? (name col)))]
                               col-def))]
              (add-columns! conn table-name cols))))))))
