(ns co.gaiwan.oak.lib.auth-middleware
  "Middleware which populates the :identity key in the request based on Bearer
  token or session

  These need to be added to the (group of) route(s) that need them. If they are
  routes end-users access in their browser, session-auth makes sense. If they
  are routes the relying party calls with a bearer token, then bearer-auth makes
  sense. Routes that don't need these should not have them.
  "
  (:require
   [clojure.string :as str]
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.domain.jwt :as jwt]
   [co.gaiwan.oak.domain.scope :as scope]
   [co.gaiwan.oak.util.routing :as routing]
   [ring.util.response :as res])
  (:import
   (java.time Instant)))

(defn- redirect-auth [{:keys [session uri] :as req} route]
  {:status 302
   :headers {"Location" (routing/url-for req route)}
   :session (assoc session :redirect-after-login uri)})

(defn- authenticated?
  "make sure the user is logged in"
  [req]
  (some? (:identity req)))

(defn get-session-auth [session type]
  (let [{:keys [authentications identity]} session]
    (some #(when (= type (:type %)) %)
          (get-in session [:authentications identity]))))

(defn update-session-auth! [session auth]
  (let [{:keys [_ identity]} session
        {:keys [type _]} auth]
    (with-meta
      (-> session
          (update-in [:authentications identity]
                     #(set (remove (fn [a] (= (:type a) type)) %)))
          (update-in [:authentications identity]
                     (fnil conj #{})
                     auth))
      {:recreate true})))

(defn before-or-equal
  "Return true if t1 <= t2"
  [t1 t2]
  (or (.isBefore ^java.time.Instant t1 t2)
      (.equals ^java.time.Instant t1 t2)))

(defn wrap-open-auth
  [h]
  (fn [{:keys [session db] :as req}]
    (let [identity-id (:identity session)
          identity    (and identity-id (identity/find-one db {:id identity-id}))
          pwd-auth    (get-session-auth session credential/type-password)
          totp-auth   (get-session-auth session credential/type-totp)
          pwd-cred    (when identity (credential/find-one db {:identity-id identity-id :type credential/type-password}))

          pwd-valid?  (and pwd-auth pwd-cred (before-or-equal (:credential/updated-at pwd-cred) (:created-at pwd-auth)))
          ]
      (if pwd-valid?
        (h (assoc req :identity identity))
        (h req)))))

(defn wrap-basic-auth
  [h]
  (fn [{:keys [session db] :as req}]
    (let [identity-id (:identity session)
          identity    (and identity-id (identity/find-one db {:id identity-id}))
          pwd-auth    (get-session-auth session credential/type-password)
          totp-auth   (get-session-auth session credential/type-totp)
          pwd-cred    (when identity (credential/find-one db {:identity-id identity-id :type credential/type-password}))
          totp-cred   (when identity (credential/find-one db {:identity-id identity-id :type credential/type-totp}))
          pwd-valid?  (and pwd-auth pwd-cred (before-or-equal (:credential/updated-at pwd-cred) (:created-at pwd-auth)))
          totp-valid? (and totp-auth totp-cred (before-or-equal (:credential/updated-at totp-cred) (:created-at totp-auth)))]

      (if pwd-valid?
        (if-not totp-cred
          (h (assoc req :identity identity))
          (if-not totp-valid?
            (redirect-auth req :totp/check)
            (h (assoc req :identity identity))))
        (redirect-auth req :auth/login)))))

(defn wrap-strong-auth
  [h]
  (fn [{:keys [session db] :as req}]
    (let [identity-id (:identity session)
          identity    (and identity-id (identity/find-one db {:id identity-id}))
          pwd-auth    (get-session-auth session credential/type-password)
          totp-auth   (get-session-auth session credential/type-totp)
          pwd-cred    (when identity (credential/find-one db {:identity-id identity-id :type credential/type-password}))
          totp-cred   (when identity (credential/find-one db {:identity-id identity-id :type credential/type-totp}))
          pwd-valid?  (and pwd-auth pwd-cred (before-or-equal (:credential/updated-at pwd-cred) (:created-at pwd-auth)))
          totp-valid? (and totp-auth totp-cred (before-or-equal (:credential/updated-at totp-cred) (:created-at totp-auth)))
          recent?     (before-or-equal (.minus (java.time.Instant/now) (java.time.Duration/ofMinutes 5))
                                       (:created-at (or totp-auth pwd-auth)))]
      (if pwd-valid?
        (if-not totp-cred
          (if-not recent?
            (redirect-auth req :auth/login)
            (h (assoc req :identity identity)))
          (if-not (and totp-valid? recent?)
            (redirect-auth req :totp/check)
            (h (assoc req :identity identity))))
        (redirect-auth req :auth/login)))))

(defn wrap-api-auth
  "Accept Authorization: Bearer style authentication.

  Will verify that the token parses according to one of our JWK, and is not expired.  "
  [h]
  (fn [{:keys [headers session db] :as req}]
    (let [auth-header    (res/get-header req "authorization")
          [scheme token] (when auth-header (str/split auth-header #"\s+"))]
      (try
        (if-let [claims (and scheme token
                             (= "bearer" (str/lower-case scheme))
                             (jwt/parse-verify db token))]
          (if (jwt/expired? claims)
            {:status 401
             :body ""}
            (h
             (assoc req
                    :identity (identity/find-one db {:id (parse-uuid (get claims "sub"))})
                    :claims claims
                    :scopes (scope/scope-set (get claims "scope")))))
          (h req))
        (catch Exception e
          (if (= :jwt-verification/failed (:type (ex-data e)))
            {:status 401
             :body ""}))))))

(defn- route-data [{:keys [request-method] :as req}]
  (let [data (-> req :reitit.core/match :data)]
    (merge (dissoc data :get :put :post :delete)
           (get data request-method))))

(defn wrap-auth-claims
  "Consolidated authorization handler with step-up

  Requires an `:auth-claim` key in the route metadata

  - `:public` - route is publicly accessible, identity is not resolved
  - `:open`   - route is publicly accessible, identity is resolved
  - `:basic`  - default level of access for web-based routes, require 2FA if
                configured, but allow password-auth if not
  - `:strong` - require a recent (< 5 min) second factor challenge, if not,
                performs a step-up
  - `:api`    - authenticate via Authorization: Bearer, session is ignored
  "
  [handler]
  (fn [req]
    (let [{:keys [auth-claim]} (route-data req)]
      (case auth-claim
        :public
        (handler req)

        :open
        ((wrap-open-auth handler) req)

        :basic
        ((wrap-basic-auth handler) req)

        :strong
        ((wrap-strong-auth handler) req)

        :api
        ((wrap-api-auth handler) req)

        (throw (ex-info (str "Illegal `:auth-claim` value: " auth-claim)
                        (:reitit.core/match req)))))))

;; TODO: use RFC7807 style error responses?
