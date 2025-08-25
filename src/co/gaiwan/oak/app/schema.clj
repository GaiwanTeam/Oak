(ns co.gaiwan.oak.app.schema
  "Database schema

  We automatically create missing tables/columns/indices when the database
  connection pool boots base on what's here."
  (:require
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identifier :as identifier]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.domain.jwk :as jwk]
   [co.gaiwan.oak.domain.oauth-authorization :as oauth-authorization]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]
   [co.gaiwan.oak.domain.oauth-code :as oauth-code]))

(def schema
  "Table defitions, order matters because of foreign key constraints"
  [[:jwk jwk/attributes]
   [:oauth_client oauth-client/attributes]
   [:identity identity/attributes]
   [:identifier identifier/attributes]
   [:credential credential/attributes]
   [:oauth_authorization oauth-authorization/attributes]
   [:oauth_code oauth-code/attributes]])

(def indices
  "Index definitions, make sure to add :if-not-exists"
  [;; Only one JWK can be default at a time
   {:create-index [[:unique :one_active_row_idx :if-not-exists]
                   [:jwk :is_default]]
    :where [[:raw "is_default"]]}

   ;; Ensures each identifier is linked to a single identity
   {:create-index [[:unique :unique_identifier_idx :if-not-exists]
                   [:identifier :type :value]]
    }])

(comment
  (user/restart! :system/database))
