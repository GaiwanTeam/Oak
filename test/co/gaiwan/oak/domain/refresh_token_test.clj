(ns co.gaiwan.oak.domain.refresh-token-test
  (:require
   [clojure.test :refer :all]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]
   [co.gaiwan.oak.domain.refresh-token :as refresh-token]
   [co.gaiwan.oak.test-harness :as harness :refer [db]]))

(use-fixtures :once (harness/with-system [:system/database]))

(use-fixtures :each
  (harness/with-auto-truncate)
  (harness/with-ensure-jwk))

(deftest refresh-token-crud-test
  (testing "Create, find, and delete refresh token"
    ;; Create OAuth client first
    (let [oauth-client (oauth-client/create! (db) {:client-name "Test Client"})
          client-id    (:oauth-client/id oauth-client)
          jwt-claims   {"sub" "user123" "iat" 123456789}
          token        (refresh-token/create! (db) {:client-id client-id :access-token-claims jwt-claims})]

      (is (string? token))
      (is (> (count token) 0))

      (let [found (refresh-token/find-one (db) token client-id)]
        (is found)
        (is (= (:refresh-token/token found) token))
        (is (= (:refresh-token/client-id found) client-id))
        (is (= (:refresh-token/access-token-claims found) jwt-claims)))

      (refresh-token/delete! (db) token client-id)

      (let [found-after-delete (refresh-token/find-one (db) token client-id)]
        (is (nil? found-after-delete))))))

(comment

  (kaocha.repl/run))
