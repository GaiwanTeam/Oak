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

(defn authenticated?
  "make sure the user is logged in"
  [req]
  (some? (:identity req)))

(defn wrap-enforce-login
  "Middleware that redirects to login page if the user is not logged in"
  [handler]
  (fn [{:keys [session] :as req}]
    (if (authenticated? req)
      (handler req)
      {:status 302
       :headers {"Location" (routing/url-for req :auth/login)}
       :session (assoc session :redirect-after-login (:uri req))})))

(comment
  (def h (wrap-enforce-login (constantly 1)))
  (h {}))

(defn get-session-auth [session type]
  (let [{:keys [authentications identity]} session]
    (some #(when (= type (:type %)) %)
          (get-in session [:authentications identity]))))

(comment
  (get-session-auth
   {:session {:authentications
              {"abc" #{{:type "password"
                        :created-at (java.time.Instant/now)}}}
              :identity "abc"}}
   credential/type-password))

;; (defn wrap-authentications
;;   "Upon a successful authentication challenge, like password or totp, we add an
;;   entry in the session under `[:authentications identity-id]` (this is a set of
;;   maps with :type and :created-at).

;;   This middleware compares the time the authentication happened to the last time
;;   the credential changed. If the credential changed after the auth happened, we
;;   reject the auth, and remove it from the session, otherwise we add it to the
;;   request under `:authn`, for easier consumption downstream.

;;   {:request-method :get
;;    :authn {\"password\" {:created-at ...}}}"
;;   [{:keys [session db] :as req}]
;;   (let [identity-id (:identity session)
;;         identity    (and identity-id (identity/find-one db {:id identity-id}))]
;;     ;; check if authentication is (still) valid
;;     ;; - if valid: add to request
;;     ;; - if invalid: remove from session
;;     (reduce
;;      (fn [acc {:keys [type created-at] :as authn}]
;;        (let [cred   (when identity (credential/find-one db {:identity-id identity-id :type type}))
;;              valid? (and cred (< (:credential/updated-at cred) created-at))]
;;          (if valid?
;;            (assoc-in req [:authn type] authn)
;;            (update-in req [:session :authentications identity-id] #(into #{} (remove #{authn}) %)))))
;;      req
;;      (get-in session [:authentications identity-id]))))

(defn wrap-session-auth
  "If an `:identity` key is set in the session, this will look up the user with
  that uuid, and set it (the identity record) as the `:identity` key on the
  request, as well as propagating the `:auth-time` key from the request to the
  session."
  [h]
  (fn [{:keys [session db] :as req}]
    (let [identity-id (:identity session)
          identity    (and identity-id (identity/find-one db {:id identity-id}))
          pwd-auth    (get-session-auth session credential/type-password)
          totp-auth   (get-session-auth session credential/type-totp)
          pwd-cred    (when identity (credential/find-one db {:identity-id identity-id :type credential/type-password}))
          totp-cred   (when identity (credential/find-one db {:identity-id identity-id :type credential/type-totp}))
          pwd-valid?  (and pwd-auth pwd-cred (.isBefore ^java.time.Instant (:credential/updated-at pwd-cred) (:created-at pwd-auth)))
          totp-valid? (and totp-auth totp-cred (.isBefore ^java.time.Instant (:credential/updated-at totp-cred) (:created-at totp-auth)))]
      (h
       ;; Logged in with password, and either no 2FA configured, or we have a
       ;; valid 2FA login
       (if (and pwd-valid? (or (not totp-cred) totp-valid?))
         (assoc req :identity identity)
         req)))))

(defn wrap-bearer-auth
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

;; TODO: use RFC7807 style error responses?
