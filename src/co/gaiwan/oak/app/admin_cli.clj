(ns co.gaiwan.oak.app.admin-cli
  "Implementation of the oakadm CLI

  Command line interface for various administrative tasks.
  "
  (:require
   [camel-snake-kebab.core :as csk]
   [charred.api :as json]
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [co.gaiwan.oak.app.admin-cli :as cli-error-mw]
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.domain.jwk :as jwk]
   [co.gaiwan.oak.domain.jwt :as jwt]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]
   [co.gaiwan.oak.util.jose :as jose]
   [lambdaisland.cli :as cli])
  (:import
   (java.io StringWriter)))

(set! *print-namespace-maps* false)

(def init {})

(defn db []
  ;; placeholder, eventually this needs to be tenant-aware
  (config/start! [:system/database])
  (:data-source (config/component :system/database)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def oauth-client-cols
  [["client_id" :oauth-client/client-id]
   ["client_secret" :oauth-client/client-secret]
   ["client_name" :oauth-client/client-name]
   ["redirect_uris" :oauth-client/redirect-uris]
   ["token_endpoint_auth_method" :oauth-client/token-endpoint-auth-method]
   ["grant_types" :oauth-client/grant-types]
   ["response_types" :oauth-client/response-types]
   ["scope" :oauth-client/scope]])

(defn create-oauth-client
  "Create a new OAuth client"
  {:flags
   ["--client-name <name>" {:doc "The name of the client"
                            :required true}
    "--redirect-uri <uri>" {:doc "Redirect URI, can be passed multiple times"
                            :coll? true
                            :key :redirect-uris}
    "--token-endpoint-auth-method <method>" {:doc "Token endpoint authentication method"
                                             :key :token-endpoint-auth-method}
    "--grant-type <type>" {:doc "Grant type, can be passed multiple times"
                           :coll? true
                           :key :grant-types}
    "--response-type <type>" {:doc "Response type, can be passed multiple times"
                              :coll? true
                              :key :response-types}
    "--scope <scope>" {:doc "Scope"
                       :coll? true}]}
  [opts]
  {:columns oauth-client-cols
   :data [(oauth-client/create!
           (db)
           (cond-> opts
             (:scope opts)
             (update :scope #(str/join " " %))))]})

(defn list-oauth-clients
  "List OAuth clients"
  [opts]
  {:columns oauth-client-cols
   :data (oauth-client/list-all (db))})

(defn delete-oauth-client [{:keys [id]}]
  (oauth-client/delete! (db) {:client-id id}))

(def oauth-client-commands
  {:doc "Read and manipulate OAuth clients"
   :commands ["create" #'create-oauth-client
              "list" #'list-oauth-clients
              "delete <client-id>" #'delete-oauth-client]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-user
  "Create a new user user"
  {:flags ["--email <email>" "Email address"
           "--password <password>" "User password"]}
  [opts]
  {:data [(identity/create-user! (db) opts)]})

(defn list-users
  "List users"
  [opts]
  {:data (identity/list-all (db))})

(defn delete-user
  "Delete user and associated identifiers/credentials"
  [opts]
  (if-let [id (parse-uuid (:id opts))]
    (identity/delete! (db) id)
    (throw (ex-info "Invalid UUID" {:id (:id opts)}))))

(def user-commands
  {:doc "Read and manipulate users"
   :commands
   ["create" #'create-user
    "list" #'list-users
    "delete <id>" #'delete-user]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def jwk-cols
  [["kid" :jwk/kid]
   ["key-type" [:jwk/public-key "kty"]]
   ["alg" [:jwk/public-key "alg"]]
   ["crv" [:jwk/public-key "crv"]]
   ["default" :jwk/is-default]
   ["created-at" :jwk/created-at]])

(defn create-jwk
  "Create a new JWK"
  {:flags ["--kty <kty>" {:doc "Key type, e.g. RSA, OKP, EC"
                          :default "OKP"}
           "--alg <alg>" {:doc "Algorithm used, must match key type."}
           "--crv <crv>" {:doc "Curve used, for EC or OKP keys"}
           "--size <size>" {:doc "Key size, for RSA keys"
                            :parse parse-long}
           "--[no-]default" {:doc "Make this the default key"}]}
  [opts]
  {:columns jwk-cols
   :data [(jwk/create! (db) (update-keys opts name))]})

(defn list-jwk
  "List JWKs"
  [opts]
  {:columns jwk-cols
   :data (jwk/list-all (db))})

(def jwk-commands {:doc      "Read and manipulate JWKs"
                   :commands ["create" #'create-jwk
                              "list" #'list-jwk]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-token
  {:flags ["--scope <scope>"  {:doc   "Scope"
                               :coll? true}
           "--client-id <client-id>" {:doc "OAuth client-id"}]}
  [opts]
  (let [sub (:user-id opts)]
    (println
     (jose/build-jwt
      (:jwk/full-key (jwk/default-key (db)))
      (jwt/access-token-claims {:issuer      (or (config/get :oak/origin)
                                                 (str "http://localhost:" (config/get :http/port)))
                                :identity-id (parse-uuid sub)
                                :client-id   (:client-id opts "")
                                :now         (System/currentTimeMillis)
                                :scope       (str/join " " (:scope opts))})))))

(def token-commands {:doc "Create JWT Bearer access tokens"
                     :commands ["create <user-id>" #'create-token]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-stop-system [handler]
  (fn [opts]
    (let [res (handler opts)]
      (config/stop!)
      res)))

(defn get-col [data col]
  (if (vector? col)
    (get-in data col)
    (get data col)))

(defn wrap-print-output [handler]
  (fn [opts]
    (let [{:keys [data columns] :as res} (handler opts)]
      (when data
        (let [columns (or columns (distinct (mapcat #(for [k (keys %)] [(name k) k]) data)))]
          (cond
            (= "json" (:format opts))
            (println (json/write-json-str (map #(update-keys % (comp csk/->snake_case name)) data)))

            (= "csv" (:format opts))
            (let [w (StringWriter.)
                  ks (distinct (mapcat keys data))]
              (json/write-csv w (cons ks
                                      (map (apply juxt (map (fn [col] #(get-col % col)) ks)) data))
                              {:close-writer? true})
              (println (.toString w)))

            :else
            (pprint/print-table (map first columns)
                                (map (fn [row]
                                       (into {} (map (fn [[h k]] [h (get-col row k)])) columns)) data)))))
      res)))

(def commands
  ["jwk" jwk-commands
   "oauth-client" oauth-client-commands
   "user" user-commands
   "token" token-commands])

(def flags
  ["--format <json|csv>" "Output JSON/CSV rather than human-readable data"
   "--show-trace" "Show full stack trace in errors"
   "-v, --verbose" "Increase verbosity"
   "-h, --help" "Show help text for a (sub-)command"])

(defn -main [& args]
  (cli/dispatch*
   {:name "oakadm"
    :init init
    :flags flags
    :commands commands
    :middleware [wrap-stop-system
                 wrap-print-output
                 cli-error-mw/wrap-error]}
   args))

;; Local Variables:
;; mode:clojure
;; End:
