(ns co.gaiwan.oak.mock
  (:require
   [charred.api :as charred]
   [clojure.string :as str]
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.jwk :as jwk]
   [co.gaiwan.oak.domain.jwt :as jwt]
   [co.gaiwan.oak.test-harness :as harness]
   [co.gaiwan.oak.util.jose :as jose]
   [ring.middleware.session.store :as ring-store]
   [ring.mock.request :as mock]
   [ring.util.response :as res]))

(def ^:dynamic *session-id* nil)

(defn request [& args]
  (cond-> (apply mock/request args)
    *session-id*
    (res/header "cookie"
                (str (harness/get-config :http-session/cookie-name)
                     "="
                     *session-id*))))

(def body mock/body)
(def json-body mock/json-body)

(alter-var-root #'ring-store/write-session
                (fn [f]
                  (fn [store key data]
                    (println "STORE" key data store)
                    (f store key data))))

(alter-var-root #'ring-store/read-session
                (fn [f]
                  (fn [store key]
                    (println "READ" key store)
                    (f store key))))

(defn swap-session! [sess-id f & args]
  (let [store (harness/session-store)]
    (ring-store/write-session
     store
     sess-id
     (apply f
            (ring-store/read-session store sess-id)
            args))))

(defn pwd-auth-sess [identity-id]
  {:identity identity-id
   :authentications {identity-id [{:type credential/type-password
                                   :created-at (java.time.Instant/now)}]}})

(defn totp-auth-sess [identity-id]
  {:identity identity-id
   :authentications {identity-id [{:type credential/type-password
                                   :created-at (java.time.Instant/now)}
                                  {:type credential/type-totp
                                   :created-at (java.time.Instant/now)}]}})

(defn build-jwt [claims]
  (jose/build-jwt (:jwk/full-key (jwk/default-key (harness/db))) claims))

(defn with-bearer [req claims]
  (assoc-in req [:headers "Authorization"] (str "Bearer " (build-jwt claims))))

(defn with-bearer-identity [req identity-id]
  (with-bearer req
    (jwt/access-token-claims
     {:issuer      "http://oak.example.com"
      :identity-id identity-id
      :client-id   "test-oauth-client"
      :scope       "profile email"})))

(defmacro with-session [initial-session-data & body]
  `(let [sess-id# (str (random-uuid))]
     (binding [*session-id* sess-id#]
       (swap-session! sess-id# (constantly ~initial-session-data))
       ~@body)))

(defn unmarshal-response-body [res]
  (let [ct (some-> (res/find-header res "content-type")
                   val
                   (str/split #";")
                   first)]
    (case ct
      "application/json"
      (update res :body charred/read-json)

      res)))

(defn fetch [req]
  (unmarshal-response-body
   ((harness/handler) req)))
