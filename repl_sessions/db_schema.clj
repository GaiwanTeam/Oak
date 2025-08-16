(ns db-schema
  (:require
   [co.gaiwan.oak.app.config :as config]
   [honey.sql :as honey]
   [next.jdbc :as jdbc]
   [next.jdbc.connection :as connection]))

(def ds (jdbc/get-datasource {:jdbcUrl "jdbc:postgresql://localhost:5432/postgres?user=postgres"}))

(def schema
  {:actor
   [[:id :uuid :primary-key]
    [:type :text] ;; human | agent | service
    [:main-account :uuid [:fk :account]]
    ]

   :identifier
   [[:id :uuid :primary-key]
    [:actor-id :uuid [:fk :actor]]
    [:type :text "email | username | phone number | external system identifier"]
    [:identifier :text]
    [:status :text "verified | unverified | sent"]
    [:is-primary :boolean "one per type can be primary"]
    ]

   :credential
   [[:id :uuid :primary-key]
    [:type :text] ;; password | OTP code
    [:credential :text]
    ]

   :account
   [[:id :uuid :primary-key]
    [:display-name :text]
    [:active :boolean]
    ]

   :keypair
   [[:id :uuid :primary-key]
    [:type :text]
    [:public_key :text]
    [:private_key :text]]
   })

(def meta
  [[:created_at :timestamptz]])

(doseq [[table cols] schema]
  (try
    (jdbc/execute! ds (honey/format {:drop-table [table] }))
    (catch Exception e))
  (jdbc/execute! ds (honey/format {:create-table [table]
                                   :with-columns (concat
                                                  (map #(vec (take 2 %)) cols)
                                                  meta)})))
