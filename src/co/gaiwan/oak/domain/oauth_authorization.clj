(ns co.gaiwan.oak.domain.oauth-authorization
  "Authorizations that have been granted through OAuth"
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.domain.scope :as scope]
   [co.gaiwan.oak.lib.db :as db]))

(def attributes
  [[:id :uuid :primary-key]
   [:identity_id :uuid [:references [:identity :id]] [:not nil]]
   [:client_id :uuid [:references [:oauth_client :id]] [:not nil]]
   [:scope :text]])

(defn create! [db {:keys [identity-id client-id scope]}]
  (db/with-transaction [conn db]
    (db/execute-honey!
     conn
     {:delete-from :oauth_authorization
      :where [:and
              [:= :identity_id identity-id]
              [:= :client_id client-id]]})
    (db/insert!
     conn
     :oauth_authorization
     {:id (uuid/v7)
      :identity_id identity-id
      :client_id client-id
      :scope scope})))

(defn exists?
  "Check if there is an authorization from the user for this client, with at least
  the requested scopes"
  [db {:keys [client-id identity-id scope]}]
  (when-let [auth (first
                   (db/execute-honey! db
                                      {:select [:*]
                                       :from :oauth_authorization
                                       :where [:and
                                               [:= :identity_id identity-id]
                                               [:= :client_id client-id]]}))]
    (scope/subset? scope (:oauth-authorization/scope auth))))

(defn delete! [db {:keys [identity-id]}]
  (db/execute-honey!
   db
   {:delete-from :oauth-authorization
    :where [:= identity-id :identity_id]}))

(defn get-apps [db]
  (db/execute-honey!
   db
   {:select [:*]
    :from [[:oauth-authorization :auth]]
    :join [[:oauth_client :client]
           [:= :auth.client_id :client.id]]}))

(comment
  (create!
   (user/db)
   {:identity-id #uuid "019a3f16-6c58-7061-ba9b-b240921b3f46"
    :client-id  #uuid "019a7182-6f33-703a-81b1-ab7cc76dc369"
    :scope "aaa"})

  (db/execute-honey!
   (user/db)
   {:select [:*]
    :from [[:oauth-authorization :auth]]
    :join [[:oauth_client :client]
           [:= :auth.client_id :client.id]]})

  (db/execute-honey!
   (user/db)
   {:select [:*]
    :from :oauth-authorization}))
