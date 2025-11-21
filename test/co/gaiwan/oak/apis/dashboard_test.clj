(ns co.gaiwan.oak.apis.dashboard-test
  (:require
   [clj-uuid :as uuid]
   [clojure.test :refer :all]
   [co.gaiwan.oak.apis.dashboard :as dashboard]
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.domain.credential :as cred]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.system.router :as router]
   [co.gaiwan.oak.test-harness :as harness]
   [co.gaiwan.oak.util.routing :as routing]
   [lambdaisland.config :as li-config]
   [reitit.coercion.malli]
   [reitit.openapi :as reitit-openapi]
   [reitit.ring.middleware.parameters :as reitit-params]
   [reitit.ring.coercion :as ring-coercion]
   [ring.middleware.flash :as ring-flash]
   [reitit.ring :as ring]
   [ring.mock.request :as mock]))
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

(deftest password-minimum-length-test
  (with-redefs [config/get (fn [k] (if (= k :password/min-length)
                                     12
                                     (li-config/get harness/config k)))]
    (let [mock-router (ring/router
                       (:routes (dashboard/component {}))
                       {:compile router/reitit-compile-fn
                        :data
                        {:coercion   (reitit.coercion.malli/create router/malli-coercion-options)
                         :muuntaja   (router/muuntaja-instance)
                         :middleware [reitit-openapi/openapi-feature
                                      reitit-params/parameters-middleware
                                      ring-coercion/coerce-exceptions-middleware
                                      ring-coercion/coerce-response-middleware
                                      ring-coercion/coerce-request-middleware
                                      ring-flash/wrap-flash]}})
          handler (ring/ring-handler mock-router)

          request (-> (mock/request :post "/dashboard/password")
                      (mock/body {:current-password *password*
                                  :new-password "short"}))
          response (handler request)]
      (is (= 400 (:status response))))))

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
