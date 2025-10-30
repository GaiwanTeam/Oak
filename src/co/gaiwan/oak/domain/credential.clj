(ns co.gaiwan.oak.domain.credential
  "Store credentials, like password hashes, authenticator app codes, OTP codes,
  nonces for passwordless login, API keys, etc"
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.lib.db :as db]
   [co.gaiwan.oak.lib.password4j :as password4j])
  (:import
   (java.time Instant)))

(set! *warn-on-reflection* true)

(def attributes
  [[:id :uuid [:primary-key]]
   [:identity_id :uuid [:references [:identity :id]] [:not nil]]
   [:type :text [:not nil]]
   [:value :text [:not nil]]
   [:expires_at :timestamptz]])

(def type-password "password")
(def type-password-reset-nonce "password_reset_nonce")
(def type-totp "Time-base one time password" "totp")

(defn create! [db {:keys [id identity-id type value expires-at] :as opts}]
  (db/insert!
   db
   :credential
   (cond-> {:id (uuid/v7)
            :identity_id identity-id
            :type type
            :value value}
     expires-at
     (assoc :expires_at expires-at))))

(defn where-sql [{:keys [id identity-id type value expired]}]
  (cond-> [:and]
    id
    (conj [:= :credential.id id])
    identity-id
    (conj [:= :credential.identity_id identity-id])
    (coll? type)
    (conj [:in :credential.type type])
    (and type (not (coll? type)))
    (conj [:= :credential.type type])
    value
    (conj [:= :credential.value value])
    (not expired)
    (conj [:or
           [:= :credential.expires_at nil]
           [:< :%now :credential.expires_at]])))

(defn find-one [db opts]
  (first (db/execute-honey! db {:select [:*]
                                :from :credential
                                :where (where-sql opts)})))

(defn set-password!
  "Set the password hash for a identity/user, creating a new credential, or
  updating the existing one."
  [db {:keys [identity-id password]}]
  (let [hsh (password4j/hash-password
             password
             (keyword (config/get :password/hash-type)))]
    (db/with-transaction [conn db]
      (db/execute-honey!
       conn
       (if-let [id (:credential/id (find-one db {:identity-id identity-id
                                                 :type "password"}))]
         {:update :credential
          :set {:value hsh
                :updated_at (Instant/now)}
          :where [:and
                  [:= :id id]
                  [:= :type "password"]]}
         {:insert-into [:credential]
          :columns [:id :identity_id :type :value]
          :values
          [[(uuid/v7)
            identity-id
            "password"
            hsh]]})))))

(defn delete! [db opts]
  (db/execute-honey! db {:delete-from :credential :where (where-sql opts)}))

(defn update! [db {:keys [id identity-id type value expires-at] :as opts}]
  (db/execute-honey! db {:update :credential
                         :set (cond-> {:identity_id identity-id
                                       :type type
                                       :value value
                                       :updated_at (Instant/now)}
                                expires-at (assoc :expires_at expires-at))
                         :where [:= id :id]}))

(defn create-or-update! [db {:keys [id identity-id type value] :as opts}]
  (if-let [record (find-one db (dissoc opts :id :value))]
    (update! db (assoc opts :id (:credential/id record)))
    (create! db opts)))

(comment
  (def tmp-id #uuid "0199e255-d5e4-7010-b2c9-435a4593af49")
  (def tmp-uuid #uuid "0199c27f-5bbe-702a-9ea8-968f6873d88e")

  (find-one (user/db) {:identity-id #uuid "0199853b-13f8-7014-b0ab-e48de68eaaab" :type "totp"})
  (delete!  (user/db) {:identity-id #uuid "0199853b-13f8-7014-b0ab-e48de68eaaab" :type "totp"})
  (create-or-update! (user/db)
                     {:id tmp-id
                      :identity-id tmp-uuid
                      :type "totp"
                      :value "sss"})
  (update! (user/db)
           {:id tmp-id
            :identity-id tmp-uuid
            :type "totp"
            :value "kkkk"})
  (create! (user/db)
           {:identity-id tmp-uuid
            :type "password"
            :value (hash-password "foo" (keyword (config/get :password/hash-type)))})

  (create! (user/db)
           {:identity-id tmp-uuid
            :type "totp_secret"
            :value secret}))
