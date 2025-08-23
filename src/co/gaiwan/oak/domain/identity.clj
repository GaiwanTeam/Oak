(ns co.gaiwan.oak.domain.identity)

(def attributes
  [[:id :uuid :primary-key]
   [:type :text [:not nil]]])
