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
        identity     (identity/create! (db) {:id identity-id})]
    (credential/set-password! (db) {:identity-id identity-id :password old-password})
    (is (match?
         {:status 303
          :flash {:success "Your password updated successfully"}
          :headers {"Location" "http://localhost/"}}
         (mock/with-session (mock/pwd-auth-sess identity-id)
           (-> (mock/request :post "/dashboard/password")
               (mock/body {:current-password     old-password
                           :new-password         new-password
                           :confirm-new-password new-password})
               (mock/fetch)))))))

;;

;; (def ^:dynamic *identity-id* nil)
;; (def ^:dynamic *password* nil)

;; (defn with-identity [f]
;;   (binding [*identity-id* (uuid/v7)
;;             *password* "secure-password-123"]
;;     (identity/create! harness/*db* {:id *identity-id*})
;;     (cred/set-password! harness/*db* {:identity-id *identity-id* :password *password*})
;;     (f)))

;; (use-fixtures :each with-identity)


;; (deftest post-password-successfully-test
;;   (with-redefs [routing/url-for (fn [req route] (case route :home/dash "/dashboard"))]
;;     (let [new-password "a-new-password-is-better"
;;           req {:db harness/*db*
;;                :identity {:identity/id *identity-id*}
;;                :parameters {:form {:current-password *password*
;;                                    :new-password new-password}}}
;;           response (dashboard/POST-password req)]
;;       (is (= 303 (:status response)))
;;       (is (= {:success "Password updated successfully"} (:flash response)))
;;       (is (cred/verify-password harness/*db* {:identity-id *identity-id* :password new-password})))))

;; (deftest post-password-wrong-current-test
;;   (with-redefs [routing/url-for (fn [req route] (case route :home/dash "/dashboard"))]
;;     (let [new-password "a-new-password-is-better"
;;           req {:db harness/*db*
;;                :identity {:identity/id *identity-id*}
;;                :parameters {:form {:current-password "wrong-password"
;;                                    :new-password new-password}}}
;;           response (dashboard/POST-password req)]
;;       (is (= 303 (:status response)))
;;       (is (= {:error "Current password is incorrect"} (:flash response)))
;;       (is (not (cred/verify-password harness/*db* {:identity-id *identity-id* :password new-password}))))))

;; (deftest post-password-same-as-old-test
;;   (with-redefs [routing/url-for (fn [req route] (case route :home/dash "/dashboard"))]
;;     (let [req {:db harness/*db*
;;                :identity {:identity/id *identity-id*}
;;                :parameters {:form {:current-password *password*
;;                                    :new-password *password*}}}
;;           response (dashboard/POST-password req)]
;;       (is (= 303 (:status response)))
;;       (is (= {:error "New password must be different from current password"} (:flash response))))))

;; (deftest password-minimum-length-test
;;   (with-redefs [routing/url-for (fn [req route] (case route :home/dash "/dashboard"))
;;                 config/get (fn [k] (if (= k :password/min-length)
;;                                      12
;;                                      (li-config/get harness/config k)))]
;;     (let [min-length (config/get :password/min-length)
;;           req {:db harness/*db*
;;                :identity {:identity/id *identity-id*}
;;                :parameters {:form {:current-password *password*
;;                                    :new-password (apply str (repeat (dec min-length) "x"))}}}
;;           response (dashboard/POST-password req)]
;;       (is (= 303 (:status response)))
;;       (is (= {:error (format "New password should not be shorter than %d characters", min-length)} (:flash response))))))

;; (deftest post-2fa-enable-test
;;   (with-redefs [routing/url-for (fn [req route] (case route :totp/setup "/totp/setup"))]
;;     (let [req {:db harness/*db*
;;                :identity {:identity/id *identity-id*}
;;                :parameters {:form {:enable-2fa true}}}
;;           response (dashboard/POST-2fa req)
;;           headers (:headers response)]
;;       (is (= 303 (:status response)))
;;       (is (= "/totp/setup" (get headers "Location"))))))

;; (deftest post-2fa-disable-test
;;   (with-redefs [routing/url-for (fn [req route] (case route :home/dash "/dashboard"))]
;;     (cred/enable-totp! harness/*db* {:identity-id *identity-id* :secret "secret"})
;;     (let [req {:db harness/*db*
;;                :identity {:identity/id *identity-id*}
;;                :parameters {:form {:enable-2fa false}}}
;;           response (dashboard/POST-2fa req)
;;           headers (:headers response)]
;;       (is (= 303 (:status response)))
;;       (is (= "/dashboard" (get headers "Location")))
;;       (is (nil? (cred/exist-totp? harness/*db* {:identity-id *identity-id*}))))))
