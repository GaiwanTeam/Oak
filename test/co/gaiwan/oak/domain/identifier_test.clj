(ns co.gaiwan.oak.domain.identifier-test
  (:require
   [clojure.test :refer :all]
   [co.gaiwan.oak.domain.identifier :as identifier]
   [co.gaiwan.oak.lib.db :as db]
   [co.gaiwan.oak.test-harness :as harness]))

(use-fixtures :once harness/with-test-database)

(deftest identifier-create-and-find-test
  (testing "Creating and finding identifiers"
    (let [identity-id (java.util.UUID/randomUUID)
          email-identifier {:identity-id identity-id
                            :type "email"
                            :value "test@example.com"
                            :primary true}
          phone-identifier {:identity-id identity-id
                            :type "phone"
                            :value "+1234567890"
                            :primary false}]

      ;; First create the identity
      (db/insert! harness/*db* :identity {:id identity-id :type "person"})

      ;; Create identifiers
      (identifier/create! harness/*db* email-identifier)
      (identifier/create! harness/*db* phone-identifier)

      ;; Test finding by type and value
      (let [found-email (identifier/find-one harness/*db* {:type "email" :value "test@example.com"})
            found-phone (identifier/find-one harness/*db* {:type "phone" :value "+1234567890"})
            all-for-identity (identifier/find-one harness/*db* {:identity-id identity-id})]

        (is found-email "Should find email identifier")
        (is found-phone "Should find phone identifier")
        (is all-for-identity "Should find at least one identifier for the identity")

        (when found-email
          (is (= "email" (:identifier/type found-email)) "Type should match")
          (is (= "test@example.com" (:identifier/value found-email)) "Value should match")
          (is (true? (:identifier/is-primary found-email)) "Primary flag should be true"))

        (when found-phone
          (is (= "phone" (:identifier/type found-phone)) "Type should match")
          (is (= "+1234567890" (:identifier/value found-phone)) "Value should match")
          (is (false? (:identifier/is-primary found-phone)) "Primary flag should be false"))))))

(deftest identifier-not-found-test
  (testing "Finding non-existent identifiers returns nil"
    (let [result (identifier/find-one harness/*db* {:type "nonexistent" :value "does@not.exist"})]
      (is (nil? result) "Should return nil for non-existent identifier"))))
