(ns co.gaiwan.oak.app.schema
  (:require
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.domain.identifier :as identifier]
   [co.gaiwan.oak.domain.jwk :as jwk]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]))

(def schema
  [[:jwk jwk/attributes]
   [:oauth_client oauth-client/attributes]
   [:identity identity/attributes]
   [:identifier identifier/attributes]
   [:credential credential/attributes]])

(def indices
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
