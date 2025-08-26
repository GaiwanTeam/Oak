(ns co.gaiwan.oak.apis.jwks
  "JSON Web Key Set

  Standard discoverable endpoint so third parties can validate our JWT tokens."
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
   ["/.well-known/jwks.json" {:name :jwks/jwks
                              :get #'GET-jwks}]})

(comment
  (user/restart! :apis/jwks))
