(ns co.gaiwan.oak.domain.credential-test
  (:require
   [clojure.test :refer :all]
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.test-harness :as harness :refer [db]]))

(use-fixtures :once (harness/with-system [:system/database]))

(use-fixtures :each
  (harness/with-auto-rollback)
  (harness/with-ensure-jwk))

(deftest credential-create-and-verify-test
  (let [email "test@example.com"
        password "password123"
        user (identity/create-user! (db) {:email email :password password})
        identity-id (:identity/id user)]
    (testing "password verification"
      (is (credential/verify-password (db) {:identity-id identity-id
                                            :password password}))
      (is (not (credential/verify-password (db) {:identity-id identity-id
                                                 :password "wrong-password"}))))))
