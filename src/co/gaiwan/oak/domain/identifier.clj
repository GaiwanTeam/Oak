(ns co.gaiwan.oak.domain.identifier
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.lib.db :as db]))

(def attributes
  [[:id :uuid [:primary-key]]
   [:identity_id :uuid [:references [:identity :id]]]
   [:type :text [:not nil]]
   [:value :text [:not nil]]
   [:is_primary :boolean [:default false]]])

(defn create! [db opts]
  (db/insert! db :identitifier
              {:id (or (:id opts) (uuid/v7))
               :type (or (:type opts) "user")}))
