(ns co.gaiwan.oak.domain.identity
  "Identity entity, can be a human identity, or non-human identity"
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identifier :as identifier]
   [co.gaiwan.oak.lib.db :as db]
   [co.gaiwan.oak.lib.password4j :as password4j]))

(def attributes
  [[:id :uuid :primary-key]
   [:type :text [:not nil]]])

(defn create! [db {:keys [id type]}]
  (db/insert! db :identity {:id (or id (uuid/v7))
                            :type (or type "user")}))

(defn create-user! [db {:keys [email password]}]
  (db/with-transaction [conn db]
    (let [ident (create! conn {:type "user"})
          id    (:identity/id ident)
          hsh   (password4j/hash-password
                 password
                 (keyword (config/get :password/hash-type)))]
      (identifier/create!
       conn
       {:identity-id id
        :type        "email"
        :value       email
        :primary     true})
      (credential/set-password-hash!
       conn
       {:identity-id   id
        :password-hash hsh})
      ident)))

(defn validate-login
  "Return identity id if password matches, nil otherwise"
  [db {:keys [identifier password]}]
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

(comment
  (create! (user/db) {})

  (create-user! (user/db)
                {:email "foo@gaiwan.co"
                 :password "hello"})

  (validate-login (user/db)
                  {:email "foo@gaiwan.co"
                   :password "hello"})
  )
