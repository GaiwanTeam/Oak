(ns co.gaiwan.oak.domain.oauth-code
  "Codes that can be exchanged in an oauth flow"
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.lib.db :as db]
   [co.gaiwan.oak.util.random :as random]))

(def attributes
  [[:id :uuid :primary-key]
   [:code :text [:not nil]]
   [:client_id :uuid [:references [:oauth_client :id]] [:not nil]]
   [:identity_id :uuid [:references [:identity :id]] [:not nil]]
   [:scope :text [:not nil]]
   [:code_challenge :text]
   [:code_challenge_method :text]
   [:auth_time :bigint]])

(defn create!
  "Create a new oauth-code, store the parameters, returns the code (string)"
  [db {:keys [client-id identity-id scope code-challenge code-challenge-method auth-time]}]
  (let [code (random/secure-base62-str 128)]
    (db/with-transaction [conn db]
      (db/execute-honey! conn
                         {:delete-from :oauth_code
                          :where [:and
                                  [:= :client_id client-id]
                                  [:= :identity_id identity-id]]})
      (db/insert! conn
                  :oauth_code
                  {:id (uuid/v7)
                   :code code
                   :client_id client-id
                   :identity_id identity-id
                   :scope scope
                   :code_challenge code-challenge
                   :code_challenge_method code-challenge-method
                   :auth_time auth-time}))
    code))

(defn where-sql [{:keys [identity-id code client-uuid]}]
  (cond-> [:and]
    identity-id
    (conj [:= :oauth_code.identity_id identity-id])
    code
    (conj [:= :oauth_code.code code])
    client-uuid
    (conj [:= :oauth_code.client_id client-uuid])
    ))

(defn find-one [db {:keys [identity-id code client-uuid] :as opts}]
  (first
   (db/execute-honey!
    db
    {:select [:*]
     :from :oauth_code
     :where (where-sql opts)
     :limit 1})))

(defn delete! [db {:keys [identity-id code client-uuid] :as opts}]
  (db/execute-honey!
   db
   {:delete-from :oauth_code
    :where (where-sql opts)}))
