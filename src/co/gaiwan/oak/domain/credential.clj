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

(defn create! [db opts]
  (db/insert!
   db
   :credential
   {:id (uuid/v7)
    :identity_id (:identity-id opts)
    :type (:type opts)
    :value (:value opts)}))

(defn get-password-hash [db identity-id]
  (:credential/value
   (first (db/execute-honey! db {:select [:value]
                                 :from :credential
                                 :where [:and
                                         [:= :identity_id identity-id]
                                         [:= :type "password"]]}))))

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
        :where  [:and
                 [:= :identity_id identity-id]
                 [:= :type "password"]]}
       {:insert-into [:credential],
        :columns [:id :identity_id :type :value],
        :values
        [[(uuid/v7)
          identity-id
          "password"
          password-hash]]}))))


(comment
  (create! (user/db)
           {:identity-id #uuid "0198db50-efe7-70f2-a03a-08cf6d462c7b"
            :type "password"
            :value (hash-password "foo" (keyword (config/get :password/hash-type)))})

  (get-password-hash (user/db) #uuid "0198db50-efe7-70f2-a03a-08cf6d462c7b")
  (set-password-hash! (user/db) #uuid "0198db50-efe7-70f2-a03a-08cf6d462c7b"
                      (hash-password "bar" (keyword (config/get :password/hash-type)))))
