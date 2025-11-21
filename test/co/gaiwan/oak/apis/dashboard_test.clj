(ns co.gaiwan.oak.apis.dashboard-test
  (:require
   [clj-uuid :as uuid]
   [clojure.test :refer :all]
   [co.gaiwan.oak.apis.dashboard :as dashboard]
   [co.gaiwan.oak.domain.credential :as cred]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.test-harness :as harness]
   [co.gaiwan.oak.util.routing :as routing]))

(use-fixtures :once harness/with-test-database)
(def ^:dynamic *identity-id* nil)
(def ^:dynamic *password* nil)
(defn with-identity [f]
  (binding [*identity-id* (uuid/v7)
            *password* "secure-password-123"]
    (identity/create! harness/*db* {:id *identity-id*})
    (cred/set-password! harness/*db* {:identity-id *identity-id* :password *password*})
    (f)))
(use-fixtures :each with-identity)

(deftest post-password-successfully-test
  (with-redefs [routing/url-for (fn [req route] (case route :home/dash "/dashboard"))]
    (let [new-password "a-new-password-is-better"
          req {:db harness/*db*
               :identity {:identity/id *identity-id*}
               :parameters {:form {:current-password *password*
                                   :new-password new-password}}}
          response (dashboard/POST-password req)]
      (is (= 303 (:status response)))
      (is (= {:success "Password updated successfully"} (:flash response)))
      (is (cred/verify-password harness/*db* {:identity-id *identity-id* :password new-password})))))

(deftest post-password-wrong-current-test
  (with-redefs [routing/url-for (fn [req route] (case route :home/dash "/dashboard"))]
    (let [new-password "a-new-password-is-better"
          req {:db harness/*db*
               :identity {:identity/id *identity-id*}
               :parameters {:form {:current-password "wrong-password"
                                   :new-password new-password}}}
          response (dashboard/POST-password req)]
      (is (= 303 (:status response)))
      (is (= {:error "Current password is incorrect"} (:flash response)))
      (is (not (cred/verify-password harness/*db* {:identity-id *identity-id* :password new-password}))))))

(deftest post-password-same-as-old-test
  (with-redefs [routing/url-for (fn [req route] (case route :home/dash "/dashboard"))]
    (let [req {:db harness/*db*
               :identity {:identity/id *identity-id*}
               :parameters {:form {:current-password *password*
                                   :new-password *password*}}}
          response (dashboard/POST-password req)]
      (is (= 303 (:status response)))
      (is (= {:error "New password must be different from current password"} (:flash response))))))

(deftest post-2fa-enable-test
  (with-redefs [routing/url-for (fn [req route] (case route :totp/setup "/totp/setup"))]
    (let [req {:db harness/*db*
               :identity {:identity/id *identity-id*}
               :parameters {:form {:enable-2fa true}}}
          response (dashboard/POST-2fa req)
          headers (:headers response)]
      (is (= 303 (:status response)))
      (is (= "/totp/setup" (get headers "Location"))))))

(deftest post-2fa-disable-test
  (with-redefs [routing/url-for (fn [req route] (case route :home/dash "/dashboard"))]
    (cred/enable-totp! harness/*db* {:identity-id *identity-id* :secret "secret"})
    (let [req {:db harness/*db*
               :identity {:identity/id *identity-id*}
               :parameters {:form {:enable-2fa false}}}
          response (dashboard/POST-2fa req)
          headers (:headers response)]
      (is (= 303 (:status response)))
      (is (= "/dashboard" (get headers "Location")))
      (is (nil? (cred/exist-totp? harness/*db* {:identity-id *identity-id*}))))))
