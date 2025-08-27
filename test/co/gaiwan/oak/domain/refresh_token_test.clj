(ns co.gaiwan.oak.domain.refresh-token-test
  (:require
   [clojure.test :refer :all]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]
   [co.gaiwan.oak.domain.refresh-token :as refresh-token]
   [co.gaiwan.oak.test-harness :as harness]))

(use-fixtures :once harness/with-test-database)

(deftest refresh-token-crud-test
  (testing "Create, find, and delete refresh token"
    ;; Create OAuth client first
    (let [oauth-client (oauth-client/create! harness/*db* {:client-name "Test Client"})
          client-id    (:oauth-client/id oauth-client)
          jwt-claims   {"sub" "user123" "iat" 123456789}
          token        (refresh-token/create! harness/*db* {:client-id client-id :jwt-claims jwt-claims})]

      (is (string? token))
      (is (> (count token) 0))

      (let [found (refresh-token/find-one harness/*db* token client-id)]
        (is found)
        (is (= (:refresh-token/token found) token))
        (is (= (:refresh-token/client-id found) client-id))
        (is (= (:refresh-token/jwt-claims found) jwt-claims)))

      (refresh-token/delete! harness/*db* token client-id)

      (let [found-after-delete (refresh-token/find-one harness/*db* token client-id)]
        (is (nil? found-after-delete))))))

(comment

  (kaocha.repl/run))
