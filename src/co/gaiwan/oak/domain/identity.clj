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

(defn create! [db opts]
  (db/insert! db :identity {:id (or (:id opts) (uuid/v7))
                            :type (or (:type opts) "user")}))

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

(defn validate-login [db {:keys [identifier password]}]
  (when-let [hsh
             (some->
              (db/execute-honey!
               db
               {:select [:credential.value]
                :from [:identity]
                :join [:identifier [:= :identity.id :identifier.identity_id]
                       :credential [:= :identity.id :credential.identity_id]]
                :where [:and
                        (identifier/where-sql {:type #{"email" "username"}
                                               :value identifier})
                        [:= :credential.type "password"]]})
              first
              :credential/value)]
    (password4j/check-password password hsh)))

(comment
  (create! (user/db) {})

  (create-user! (user/db)
                {:email "foo@gaiwan.co"
                 :password "hello"})

  (validate-login (user/db)
                  {:email "foo@gaiwan.co"
                   :password "hello"})
  )
