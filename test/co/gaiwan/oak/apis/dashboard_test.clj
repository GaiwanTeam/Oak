(ns co.gaiwan.oak.apis.dashboard-test
  (:require
   [clj-uuid :as uuid]
   [clojure.test :refer :all]
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.mock :as mock]
   [co.gaiwan.oak.test-harness :as harness :refer [db]]
   [co.gaiwan.oak.util.random :as random]))

(require 'matcher-combinators.test)

(use-fixtures :once
  (harness/with-system [:system/handler]))

(use-fixtures :each
  (harness/with-auto-rollback)
  (harness/with-ensure-jwk))

(deftest password-reset-happy-path
  (let [identity-id  (uuid/v7)
        old-password (random/secure-base62-str 128)
        new-password (random/secure-base62-str 128)
        identity     (identity/create! (db) {:id identity-id})
        http-origin  (harness/get-config :http/origin)]
    (credential/set-password! (db) {:identity-id identity-id :password old-password})
    (is (match?
         {:status 303
          :flash {:success "Your password updated successfully"}
          :headers {"Location" (str http-origin "/")}}
         (mock/with-session (mock/pwd-auth-sess identity-id)
           (-> (mock/request :post (str http-origin "/dashboard/password"))
               (mock/body {:current-password     old-password
                           :new-password         new-password
                           :confirm-new-password new-password})
               (mock/fetch)))))))

(deftest password-reset-wrong-password
  (let [identity-id  (uuid/v7)
        old-password (random/secure-base62-str 128)
        wrong-password (random/secure-base62-str 128)
        new-password (random/secure-base62-str 128)
        http-origin  (harness/get-config :http/origin)
        identity     (identity/create! (db) {:id identity-id})]
    (credential/set-password! (db) {:identity-id identity-id :password old-password})
    (let [resp (mock/with-session (mock/pwd-auth-sess identity-id)
                 (-> (mock/request :post (str http-origin "/dashboard/password"))
                     (mock/body {:current-password     wrong-password
                                 :new-password         new-password
                                 :confirm-new-password new-password})
                     (mock/fetch)))]
      (is (match? {:status 400} resp))
      (is (re-find #"The current password you entered is incorrect" (:body resp)))
      (is (credential/verify-password (db) {:identity-id identity-id :password old-password})))))

(deftest password-reset-same-as-old
  (let [identity-id  (uuid/v7)
        old-password (random/secure-base62-str 128)
        new-password old-password
        http-origin  (harness/get-config :http/origin)
        identity     (identity/create! (db) {:id identity-id})]
    (credential/set-password! (db) {:identity-id identity-id :password old-password})
    (let [resp (mock/with-session (mock/pwd-auth-sess identity-id)
                 (-> (mock/request :post (str http-origin "/dashboard/password"))
                     (mock/body {:current-password     old-password
                                 :new-password         new-password
                                 :confirm-new-password new-password})
                     (mock/fetch)))]
      (is (match? {:status 400} resp))
      (is (re-find #"The new password you chose is identical to old password" (:body resp)))
      (is (credential/verify-password (db) {:identity-id identity-id :password old-password})))))

(deftest password-reset-minimum-length
  (let [identity-id  (uuid/v7)
        old-password (random/secure-base62-str 128)
        min-length   (harness/get-config :password/min-length)
        new-password (subs (random/secure-base62-str 128) 0 (dec min-length))
        http-origin  (harness/get-config :http/origin)
        identity     (identity/create! (db) {:id identity-id})]
    (credential/set-password! (db) {:identity-id identity-id :password old-password})
    (let [resp (mock/with-session (mock/pwd-auth-sess identity-id)
                 (-> (mock/request :post (str http-origin "/dashboard/password"))
                     (mock/body {:current-password     old-password
                                 :new-password         new-password
                                 :confirm-new-password new-password})
                     (mock/fetch)))]
      (is (match? {:status 400} resp))
      (is (re-find #"The new password should be at least" (:body resp)))
      (is (credential/verify-password (db) {:identity-id identity-id :password old-password})))))

(deftest post-2fa-enable
  (let [identity-id  (uuid/v7)
        old-password (random/secure-base62-str 128)
        http-origin  (harness/get-config :http/origin)
        identity (identity/create! (db) {:id identity-id})]
    (credential/set-password! (db) {:identity-id identity-id :password old-password})
    (is (match? {:status 303
                 :headers {"Location" (str http-origin "/2fa/setup")}}
                (mock/with-session (mock/pwd-auth-sess identity-id)
                  (-> (mock/request :post (str http-origin "/dashboard/2fa"))
                      (mock/body {:enable-2fa true})
                      (mock/fetch)))))))

(deftest post-2fa-disable
  (let [identity-id  (uuid/v7)
        old-password (random/secure-base62-str 128)
        secret (random/secure-base62-str 128)
        http-origin  (harness/get-config :http/origin)
        identity (identity/create! (db) {:id identity-id})]
    (credential/set-password! (db) {:identity-id identity-id :password old-password})
    (credential/enable-totp! (db) {:identity-id identity-id :secret secret})
    (let [resp (mock/with-session (mock/totp-auth-sess identity-id)
                 (-> (mock/request :post (str http-origin "/dashboard/2fa"))
                     (mock/body {:enable-2fa false})
                     (mock/fetch)))]
      (is (match? {:status 303
                   :headers {"Location" (str http-origin "/")}} resp))
      (is (nil? (credential/exist-totp? (db) {:identity-id identity-id}))))))
