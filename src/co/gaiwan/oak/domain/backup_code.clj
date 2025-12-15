(ns co.gaiwan.oak.domain.backup-code
  "Backup code that used together with 2FA"
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.lib.db :as db]
   [co.gaiwan.oak.lib.password4j :as password4j]
   [co.gaiwan.oak.util.random :as random]))

(def attributes
  [[:id :uuid :primary-key]
   [:identity_id :uuid [:references [:identity :id]] [:not nil]]
   [:code_hash :text [:not nil]]
   [:used :boolean [:not nil]]])

(defn create!
  [db {:keys [_ identity-id code-hash]}]
  (db/insert!
   db
   :backup_code
   (cond-> {:id (uuid/v7)
            :identity_id identity-id
            :code_hash code-hash
            :used false})))

(defn where-sql [{:keys [id identity-id code-hash used]}]
  (cond-> [:and]
    id
    (conj [:= :backup_code.id id])
    identity-id
    (conj [:= :backup_code.identity_id identity-id])
    code-hash
    (conj [:= :backup_code.code_hash code-hash])
    (not (nil? used))
    (conj [:= :backup_code.used used])))

(defn find-one [db opts]
  (first (db/execute-honey! db {:select [:*]
                                :from :backup_code
                                :where (where-sql opts)})))

(defn find-all [db opts]
  (db/execute-honey! db {:select [:*]
                         :from :backup_code
                         :where (where-sql opts)}))

(def backup-code-count 10)

(defn gen-code []
  (random/secure-base62-str 56))

(defn gen-codes []
  (vec
   (doall
    (repeatedly backup-code-count gen-code))))

(defn code->hash [code]
  (password4j/hash-password
   code
   (keyword (config/get :password/hash-type))))

;;;;;;;;;;;; API ;;;;;;;;;;;;

(defn valid-count
  [db {:keys [identity-id]}]
  (let [entities (find-all db {:identity-id identity-id
                               :used false})]
    (count entities)))

(defn verify-code
  "verify code to identity-id"
  [db {:keys [identity-id code]}]
  (let [entities (find-all db {:identity-id identity-id
                               :used false})]
    (some
     (fn [{:backup-code/keys [id code-hash]}]
       (when (password4j/check-password code code-hash)
         id))
     entities)))

(defn generate!
  "generate the backup codes and store their hash"
  [db {:keys [identity-id]}]
  (let [codes (gen-codes)
        hashes (map
                code->hash
                codes)]
    (db/with-transaction [conn db]
      (db/execute-honey!
       conn
       {:insert-into [:backup_code]
        :columns [:id :identity_id :code_hash :used]
        :values
        (mapv
         (fn [hsh]
           [(uuid/v7)
            identity-id
            hsh
            false])
         hashes)}))
    codes))

(defn delete! [db opts]
  (db/execute-honey! db {:delete-from :backup_code :where (where-sql opts)}))

(defn update! [db {:keys [id :used] :as opts}]
  (db/execute-honey! db {:update :backup_code
                         :set {:used used}
                         :where [:= id :id]}))

(comment
  (def tmp-id #uuid "019a3f16-6c58-7061-ba9b-b240921b3f46")
  (def code "4HxJHEErEv")

  (def id (verify-code (user/db) {:identity-id tmp-id :code code}))
  (update! (user/db) {:id id :used true})

  (generate! (user/db) {:identity-id tmp-id}))
