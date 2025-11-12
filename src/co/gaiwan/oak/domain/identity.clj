(ns co.gaiwan.oak.domain.identity
  "Identity entity, can be a human identity, or non-human identity"
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identifier :as identifier]
   [co.gaiwan.oak.domain.oauth-authorization :as oauth-authorization]
   [co.gaiwan.oak.domain.oauth-code :as oauth-code]
   [co.gaiwan.oak.lib.db :as db]
   [co.gaiwan.oak.lib.password4j :as password4j]))

(def attributes
  [[:id :uuid :primary-key]
   [:type :text [:not nil]]
   ;; freeform extra jwt claims, escape hatch
   [:claims :jsonb]])

(defn create! [db {:keys [id type claims]}]
  (db/insert! db :identity {:id (or id (uuid/v7))
                            :type (or type "user")
                            :claims claims}))

(defn create-user! [db {:keys [email password claims]}]
  (db/with-transaction [conn db]
    (let [ident (create! conn {:type "user" :claims claims})
          id    (:identity/id ident)]
      (identifier/create!
       conn
       {:identity-id id
        :type        "email"
        :value       email
        :primary     true})
      (when password
        (credential/set-password!
         conn
         {:identity-id   id
          :password password}))
      ident)))

(defn update! [db {:keys [id type claims]}]
  (when (or type claims)
    (db/update!
     db
     {:update :identity
      :where [:= :id id]
      :set (cond-> {}
             type (assoc :type type)
             claims (assoc :claims [:lift claims]))})))

(defn list-all [db]
  (doall
   (for [i (db/execute-honey! db {:select [:*] :from :identity})]
     (reduce
      (fn [i {:identifier/keys [type value]}]
        (update i (keyword type) (fnil conj []) value))
      i
      (db/execute-honey! db {:select [:*]
                             :from :identifier
                             :where [:= (:identity/id i) :identifier.identity_id]})))))

(defn validate-login
  "Return identity id if password matches, nil otherwise"
  [db {:keys [identifier password] :as opts}]
  (when-let [identity
             (some->
              (db/execute-honey!
               db
               {:select [:identity.id :credential.value]
                :from [:identity]
                :join [:identifier [:= :identity.id :identifier.identity_id]
                       :credential [:= :identity.id :credential.identity_id]]
                :where [:and
                        (identifier/where-sql {:type #{"email" "username"}
                                               :value identifier})
                        [:= :credential.type "password"]]})
              first)]
    (let [hsh (:credential/value identity)
          id (:identity/id identity)]
      (when (password4j/check-password password hsh)
        id))))

(defn reset-password-with-nonce!
  [db {:keys [nonce password]}]
  (if-let [{:keys [nonce-id identity-id]} (credential/resolve-password-reset-nonce db nonce)]
    (do
      (db/with-transaction [conn db]
        (credential/delete! conn {:id nonce-id})
        (credential/set-password! conn {:identity-id identity-id :password password}))
      true)
    false))

(defn delete! [db id]
  (db/with-transaction [conn db]
    (let [sel {:identity-id id}]
      (identifier/delete! conn sel)
      (credential/delete! conn sel)
      (oauth-authorization/delete! conn sel)
      (oauth-code/delete! conn sel)
      (db/execute-honey! conn {:delete-from :identity :where [:= id :id]}))))

(defn find-one [db {:keys [id]}]
  (when-let [identity (first (db/execute-honey! db {:select [:*]
                                                    :from :identity
                                                    :where [:= :id id]}))]
    identity
    #_(reduce
       (fn [identity {:identifier/keys [type] :as identifier}]
         (update identity (keyword "identity" type) (fnil conj []) identifier))
       identity
       (identifier/find-all db {:identity-id id}))))

(comment
  (create! (user/db) {})

  (create-user! (user/db)
                {:email "foo@gaiwan.co"
                 :password "hello"})

  (def tmp-uuid #uuid "0199c27f-5bbe-702a-9ea8-968f6873d88e")
  (find-one (user/db) {:id tmp-uuid})
  (:identity/id (find-one (user/db) {:id tmp-uuid}))

  (validate-login (user/db)
                  {:email "foo@gaiwan.co"
                   :password "hello"}))
