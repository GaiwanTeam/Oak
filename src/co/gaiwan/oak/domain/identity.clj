(ns co.gaiwan.oak.domain.identity
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.lib.db :as db]))

(def attributes
  [[:id :uuid :primary-key]
   [:type :text [:not nil]]])

(defn create! [db opts]
  (db/insert! db :identity {:id (or (:id opts) (uuid/v7))
                            :type (or (:type opts) "user")}))
