(ns co.gaiwan.oak.domain.auth-backend
  "Domain layer for working with id/pw login"
  (:require
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identity :as identity]
   [buddy.auth.protocols :as proto]
   [buddy.auth.http :as http]
   [buddy.auth :refer [authenticated?]]))

(defn get-session-auth [session type]
  (let [{:keys [identity]} session]
    (some #(when (= type (:type %)) %)
          (get-in session [:authentications identity]))))

(comment
  (get-session-auth
   {:session {:authentications
              {"abc" #{{:type "password"
                        :created-at (java.time.Instant/now)}}}
              :identity "abc"}}
   credential/type-password))

(defn parsefn
  "parsefn will parse the session and return the authdata from session"
  [{:keys [session db]}]
  (let [pwd-auth  (get-session-auth session credential/type-password)
        totp-auth (get-session-auth session credential/type-totp)]
    {:identitiy-id  (:identity session)
     :db db
     :pwd-auth pwd-auth
     :totp-auth totp-auth}))

(defn authfn
  "authfn will return the data which will be put into the `:identity` key of a request"
  [{:keys [identity-id db pwd-auth totp-auth]}]
  (let [pwd-cred    (when identity (credential/find-one db {:identity-id identity-id :type credential/type-password}))
        totp-cred   (when identity (credential/find-one db {:identity-id identity-id :type credential/type-totp}))
        pwd-valid?  (and pwd-auth pwd-cred (.isBefore ^java.time.Instant (:credential/updated-at pwd-cred) (:created-at pwd-auth)))
        totp-valid? (and totp-auth totp-cred (.isBefore ^java.time.Instant (:credential/updated-at totp-cred) (:created-at totp-auth)))
        identity    (and identity-id (identity/find-one db {:id identity-id}))]
    (when (and pwd-valid? (or (not totp-cred) totp-valid?))
      identity)))

(defn password-backend
  [& [{:keys [unauthorized-handler parsefn authfn] :or {authfn identity}}]]
  (reify
    proto/IAuthentication
    (-parse [_ request]
      (parsefn request))

    (-authenticate [_ request data]
      (authfn data))

    proto/IAuthorization
    (-handle-unauthorized [_ request metadata]
      (if unauthorized-handler
        (unauthorized-handler request metadata)
        (if (authenticated? request)
          (http/response "Permission denied" 403)
          (http/response "Unauthorized" 401))))))

(comment
  ;; Using password-backend

  (password-backend {:parsefn parsefn
                     :authtn  authfn}))
