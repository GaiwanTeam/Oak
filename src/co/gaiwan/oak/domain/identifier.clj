(ns co.gaiwan.oak.domain.identifier
  "An identifier identifies an identity

  Email, phone number, etc"
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.lib.db :as db]))

(def attributes
  [[:id :uuid [:primary-key]]
   [:identity_id :uuid [:references [:identity :id]] [:not nil]]
   [:type :text [:not nil]]
   [:value :text [:not nil]]
   [:is_verified :boolean [:default false]]
   [:is_primary :boolean [:default false]]])

(defn create! [db opts]
  (db/insert!
   db :identifier
   {:id (or (:id opts) (uuid/v7))
    :identity_id (:identity-id opts)
    :type (:type opts)
    :value (:value opts)
    :is_primary (boolean (:primary opts))}))

(defn where-sql [{:keys [identity-id type value primary]}]
  (cond-> [:and]
    identity-id
    (conj [:= :identifier.identity_id identity-id])
    (coll? type)
    (conj [:in :identifier.type type])
    (and type (not (coll? type)))
    (conj [:= :identifier.type type])
    value
    (conj [:= :identifier.value value])
    primary
    (conj [:= :identifier.primary value])))

(defn find-sql [{:keys [identity-id type value primary] :as opts}]
  {:select [:*]
   :from :identifier
   :where (where-sql opts)})

(defn find-one [db {:keys [identity-id type value primary] :as opts}]
  (first
   (db/execute-honey! db (find-sql opts))))

(comment
  (find-one (user/db) {:type "email" :value "foo@bar.com"}))
