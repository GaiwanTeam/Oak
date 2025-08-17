(ns repl-sessions.db-schema
  (:require
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.util.jose :as jose]
   [honey.sql :as honey]
   [lambdaisland.makina.app :as makina]
   [next.jdbc :as jdbc]
   [next.jdbc.connection :as connection]))


(config/start! [:database])
(config/refresh)

(def ds (:data-source (makina/component config/system :database)))
(def ds (jdbc/get-datasource {:jdbcUrl "jdbc:postgresql://localhost:5432/oak?user=oak&password=oak"}))

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

   :jwk
   [[:kid :uuid :primary-key]
    [:public_key :jsonb]
    [:full_key :jsonb]
    [:is_default :boolean]
    [:revoked_at :timestamptz]]
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

(let [k (jose/new-jwk {"kty" "OKP" "crv" "Ed25519"})
      p (jose/public-parts k)]
  (honey/format {:insert-into :jwk
                 :columns [:kid :public_key :full_key]
                 :values [[[:cast (get k "kid") :uuid] [:lift p] [:lift k]]]}))


[[:kid :uuid :primary-key]
 [:public_key :jsonb]
 [:full_key :jsonb]
 [:is_default :boolean]
 [:revoked_at :timestamptz]]
