(ns co.gaiwan.oak.domain.identifier)

(def attributes
  [[:id :uuid [:primary-key]]
   [:identity_id :uuid [:references [:identity :id]]]
   [:type :text [:not nil]]
   [:value :text [:not nil]]
   [:is_primary :boolean [:default false]]])
