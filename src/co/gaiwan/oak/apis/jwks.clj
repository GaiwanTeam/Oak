(ns co.gaiwan.oak.apis.jwks
  (:require
   [co.gaiwan.oak.util.log :as log]
   [honey.sql :as honey]
   [next.jdbc :as jdbc]))

(defn execute! [db qry]
  (jdbc/execute! db (honey/format qry)))

(defn GET-jwks
  [{:keys [db] :as req}]
  {:body
   {:keys
    (map :jwk/public_key
         (execute!
          db
          {:select [:public_key] :from [:jwk]}))}})

(defn component [opts]
  {:routes
   ["/.well-known/jwks.json" {:get #'GET-jwks}]})

(comment
  (user/restart! :apis/jwks))
