(ns co.gaiwan.oak.domain.jwk
  "Domain layer for working with JWKs"
  (:require
   [co.gaiwan.oak.util.jose :as jose]
   [co.gaiwan.oak.lib.db :as db]))

(def attributes
  [[:kid :uuid :primary-key]
   [:public_key :jsonb [:not nil]]
   [:full_key :jsonb [:not nil]]
   [:is_default :boolean [:default false]]
   [:revoked_at :timestamptz]])

(def key-defaults
  {"RSA" {"size" 2048}
   "EC"  {"alg" "ES256" "crv" "P-256"}
   "OKP" {"crv" "Ed25519"}})

(defn create!
  "Create a new JWK and insert into the DB, options as per [[jose/new-jwk]].
  `opts` uses string keys."
  [db opts]
  {:pre [(get opts "kty")]}
  (let [k (jose/new-jwk (merge (get key-defaults (get opts "kty"))
                               opts))
        p (jose/public-parts k)]
    (db/execute-honey!
     db
     {:insert-into :jwk
      :columns [:kid :public_key :full_key]
      :values [[[:cast (get k "kid") :uuid] [:lift p] [:lift k]]]})
    k))

(defn list-all [db]
  (map :jwk/public-key
       (db/execute-honey!
        db
        {:select [:public_key]
         :from :jwk})))
