(ns co.gaiwan.oak.domain.credential-test
  (:require
   [clojure.test :refer :all]
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.test-harness :as harness]))

(use-fixtures :once harness/with-test-database)

(deftest credential-create-and-verify-test
  (let [email "test@example.com"
        password "password123"
        user (identity/create-user! harness/*db* {:email email :password password})
        identity-id (:identity/id user)]
    (testing "password verification"
      (is (credential/verify-password harness/*db* {:identity-id identity-id
                                                    :password password}))
      (is (not (credential/verify-password harness/*db* {:identity-id identity-id
                                                         :password "wrong-password"}))))))