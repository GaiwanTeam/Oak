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
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.domain.jwt :as jwt]
   [co.gaiwan.oak.domain.scope :as scope]
   [co.gaiwan.oak.util.routing :as routing]
   [ring.util.response :as res]))

(defn authenticated?
  "make sure the user is logged in"
  [req]
  (some? (:identity req)))

(defn wrap-enforce-login
  "Middleware that redirects to login page if the user is not logged in"
  [handler]
  (fn [req]
    (if (authenticated? req)
      (handler req)
      {:status 302
       :headers {"Location" (routing/url-for req :auth/login)}
       :session {:redirect-after-login (:uri req)}})))

(comment
  (def h (wrap-enforce-login (constantly 1)))
  (h {}))

(defn wrap-session-auth
  "If an `:identity` key is set in the session, this will look up the user with
  that uuid, and set it (the identity record) as the `:identity` key on the
  request, as well as propagating the `:auth-time` key from the request to the
  session."
  [h]
  (fn [{:keys [headers session db] :as req}]
    (let [auth-header (res/get-header req "authorization")
          [scheme token] (when auth-header (str/split auth-header #"\s+"))]
      (h
       (if-let [id (and (not (:2fa-checking session))
                        (:identity session)
                        (identity/find-one db {:id (:identity session)}))]
         (assoc req
                :identity id
                :auth-time (:auth-time session))
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
