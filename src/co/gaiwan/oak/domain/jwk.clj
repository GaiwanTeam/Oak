(ns co.gaiwan.oak.domain.jwk
  "Domain layer for working with JWKs"
  (:require
   [clj-uuid :as uuid]
   [clojure.zip :as str]
   [co.gaiwan.oak.lib.db :as db]
   [co.gaiwan.oak.util.jose :as jose])
  (:import
   (java.security NoSuchAlgorithmException)))

(def attributes
  [[:id :uuid :primary-key]
   [:kid :text :unique [:not nil]]
   [:public_key :jsonb [:not nil]]
   [:full_key :jsonb [:not nil]]
   [:is_default :boolean [:default false]]
   [:revoked_at :timestamptz]])

(def key-defaults
  {"RSA" {"size" 2048
          "alg" "RS256"}
   "EC" {"alg" "ES256" "crv" "P-256"}
   "OKP" {"crv" "Ed25519"}})

(defn create!
  "Create a new JWK and insert into the DB, options as per [[jose/new-jwk]].
  `opts` uses string keys."
  [db opts]
  {:pre [(get opts "kty")]}
  (let [k (jose/new-jwk (merge (get key-defaults (get opts "kty"))
                               opts))
        p (jose/public-parts k)
        default (get opts "default")]
    (db/with-transaction [conn db]
      (let [jwk-count (-> (db/execute-honey!
                           conn
                           {:select [[[:count :*]]] :from :jwk})
                          first :count)
            default (if (some? default)
                      default
                      (= 0 jwk-count))]
        (when (and default (not= 0 jwk-count))
          (db/execute-honey!
           conn
           {:update :jwk
            :set {:is_default false}}))
        (db/insert!
         conn
         :jwk
         {:id (uuid/v7)
          :kid (get k "kid")
          :public_key p
          :full_key k
          :is_default default})))))

(defn where-sql [{:keys [kid include-revoked]}]
  (cond-> [:and]
    kid
    (conj [:= :jwk.kid kid])
    (not include-revoked) ;; default to not including revoked keys
    (conj [:= :jwk.revoked_at nil])))

(defn list-all [db]
  (db/execute-honey!
   db
   {:select [:id :kid :is_default :public_key :created_at] ;; don't select full_key by default
    :from :jwk}))

(defn delete! [db kid]
  (db/execute-honey!
   db
   {:delete-from :jwk
    :where [:= :kid kid]}))

(defn default-key [db]
  (first
   (db/execute-honey!
    db
    {:select [:full_key]
     :from :jwk
     :where [:= :is_default true]
     :limit 1})))

(defn hash-alg [{:strs [kty crv alg]}]
  (cond
    (#{"RSA" "EC"} kty)
    (str "SHA-" (str/replace alg #"^\D*" ""))
    (= "OKP" kty)
    (if (= "Ed25519" crv)
      "SHA-512"
      (throw (NoSuchAlgorithmException. "Ed448/SHAKE256 Not Implemented")))))

(defn find-one [db opts]
  (first
   (db/execute-honey! db {:select [:*]
                          :from :jwk
                          :where (where-sql opts)})))
