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
