(ns co.gaiwan.oak.apis.jwks
  (:require
   [co.gaiwan.oak.util.log :as log]
   [honey.sql :as honey]
   [next.jdbc :as jdbc]))

(defn execute! [db qry]
  (jdbc/execute! db (honey/format qry)))

(def GET-jwks
  {:handler
   (fn [{:keys [db] :as req}]
     (log/warn :JWKS true)
     {:body
      {:keys
       (map :jwk/public_key
            (execute!
             db
             {:select [:public_key] :from [:jwk]}))}})})

(defn component [opts]
  (log/info :message "Starting OAuth API")
  {:routes
   ["/.well-known/jwks.json" {:get GET-jwks}]})

(comment
  (user/restart! :apis/jwks))
