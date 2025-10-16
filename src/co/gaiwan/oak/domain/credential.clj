(ns co.gaiwan.oak.domain.credential
  "Store credentials, like password hashes, authenticator app codes, OTP codes,
  nonces for passwordless login, API keys, etc"
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.lib.db :as db]))

(set! *warn-on-reflection* true)

(def attributes
  [[:id :uuid [:primary-key]]
   [:identity_id :uuid [:references [:identity :id]] [:not nil]]
   [:type :text [:not nil]]
   [:value :text [:not nil]]])

(defn create! [db {:keys [id identity-id type value] :as opts}]
  (db/insert!
   db
   :credential
   {:id (uuid/v7)
    :identity_id identity-id
    :type type
    :value value}))

(defn get-hash [db identity-id type]
  (:credential/value
   (first (db/execute-honey! db {:select [:value]
                                 :from :credential
                                 :where [:and
                                         [:= :identity_id identity-id]
                                         [:= :type type]]}))))

(defn get-password-hash [db identity-id]
  (get-hash db identity-id "password"))

(defn set-password-hash!
  "Set the password hash for a identity/user, creating a new credential, or
  updating the existing one."
  [db {:keys [identity-id password-hash]}]
  (db/with-transaction [conn db]
    (db/execute-honey!
     conn
     (if (get-password-hash conn identity-id)
       {:update :credential
        :set {:value password-hash}
        :where [:and
                [:= :identity_id identity-id]
                [:= :type "password"]]}
       {:insert-into [:credential],
        :columns [:id :identity_id :type :value],
        :values
        [[(uuid/v7)
          identity-id
          "password"
          password-hash]]}))))

(defn delete! [db {:keys [identity-id]}]
  (db/execute-honey! db {:delete-from :credential :where [:= identity-id :identity_id]}))

(defn find-one [db identity-id type]
  (first (db/execute-honey! db {:select [:*]
                                :from :credential
                                :where [:and
                                        [:= :identity_id identity-id]
                                        [:= :type type]]})))

(defn update! [db {:keys [id identity-id type value] :as opts}]
  (db/execute-honey! db {:update :credential
                         :set {:identity_id identity-id
                               :type type
                               :value value}
                         :where [:= id :id]}))

(defn create-or-update! [db {:keys [id identity-id type value] :as opts}]
  (if-let [record (find-one db identity-id type)]
    (update! db (assoc opts :id (:credential/id record)))
    (create! db opts)))

(comment
  (def tmp-id #uuid "0199e255-d5e4-7010-b2c9-435a4593af49")
  (def tmp-uuid #uuid "0199c27f-5bbe-702a-9ea8-968f6873d88e")

  (create-or-update! (user/db)
                     {:id tmp-id
                      :identity-id tmp-uuid
                      :type "totp"
                      :value "jjjj"})
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
            :value secret})

  (get-hash (user/db) tmp-uuid "totp")
  (get-hash (user/db) tmp-uuid "password")
  (get-password-hash (user/db) tmp-uuid)
  (set-password-hash! (user/db) tmp-uuid
                      (hash-password "bar" (keyword (config/get :password/hash-type)))))
