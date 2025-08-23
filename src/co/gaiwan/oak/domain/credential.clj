(ns co.gaiwan.oak.domain.credential
  (:require
   [clj-uuid :as uuid]
   [co.gaiwan.oak.lib.db :as db]))

(def attributes
  [[:id :uuid [:primary-key]]
   [:identity_id :uuid [:references [:identity :id]]]
   [:type :text [:not nil]]
   [:value :text [:not nil]]])

(defn create-credential! [db opts]
  (db/insert!
   db
   "credential"
   {:id (uuid/v7)})
  )
