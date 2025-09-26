(ns co.gaiwan.oak.domain.refresh-token
  "Refresh tokens for OAuth flow"
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.lib.db :as db]
   [co.gaiwan.oak.util.random :as random]))

(def attributes
  [[:id :uuid :primary-key]
   [:token :text [:not nil]]
   [:client_id :uuid [:references [:oauth_client :id]] [:not nil]]
   [:access_token_claims :jsonb [:not nil]]
   [:id_token_claims :jsonb]])

(defn create!
  "Create a new refresh token with JWT claims"
  [db {:keys [client-id access-token-claims id-token-claims]}]
  (let [token (random/secure-base62-str 128)]
    (db/insert! db
                :refresh_token
                {:id (uuid/v7)
                 :token token
                 :client_id client-id
                 :access_token_claims access-token-claims
                 :id_token_claims id-token-claims})
    token))

(defn find-one [db token client-id]
  (first
   (db/execute-honey!
    db
    {:select [:*]
     :from :refresh_token
     :where [:and
             [:= :token token]
             [:= :client_id client-id]]
     :limit 1})))

(defn delete! [db token client-id]
  (db/execute-honey!
   db
   {:delete-from :refresh_token
    :where [:and
            [:= :token token]
            [:= :client_id client-id]]}))

(comment
  (def client
    (co.gaiwan.oak.domain.oauth-client/create! (user/db) {:client-name "test-client"}))

  (def client-id  (:oauth-client/id client))
  (def token   (create! (user/db) {:client-id client-id :jwt-claims {:sub "user123" :iat 123456789}}))
  (find-one (user/db) token client-id)
  (delete! (user/db) token client-id))
