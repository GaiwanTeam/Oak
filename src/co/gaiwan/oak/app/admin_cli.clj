(ns co.gaiwan.oak.app.admin-cli
  "Implementation of the oakadm CLI

  Command line interface for various administrative tasks.
  "
  (:gen-class)
  (:require
   [clojure.string :as str]
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identifier :as identifier]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.domain.jwk :as jwk]
   [co.gaiwan.oak.domain.jwt :as jwt]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]
   [co.gaiwan.oak.lib.cli-error-mw :as cli-error-mw]
   [co.gaiwan.oak.lib.db :as db]
   [lambdaisland.cli :as cli])
  (:import
   (java.io StringWriter)))

(try
  (set! *print-namespace-maps* false)
  (catch Exception e
    (alter-var-root #'*print-namespace-maps* (constantly false))))

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

(defn update-oauth-client
  {:doc
   "Change one or more OAuth2 client settings.

   Use either --id or --client-id to identify the client."
   :flags
   ["--id <uuid>" {:doc "UUID identifying this client"}
    "--client-id <oauth2-id>" {:doc "OAuth2 identifier"}
    "--client-name <name>" {:doc "The name of the client"}
    "--client-secret" {:doc "Generate a new client secret"}
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
  (oauth-client/update! (db) (cond-> opts
                               (:client-secret opts)
                               (assoc :client-secret (oauth-client/new-client-secret))
                               (:scope opts)
                               (update :scope #(str/join " " %)))))

(def oauth-client-commands
  {:doc "Read and manipulate OAuth clients"
   :commands ["create" #'create-oauth-client
              "list" #'list-oauth-clients
              "update" #'update-oauth-client
              "delete <client-id>" #'delete-oauth-client]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-user
  "Create a new user"
  {:flags ["--email <email>" {:doc "Email address"
                              :required true}
           "--password <password>" "User password"
           "--claim <k=v>" {:doc "Additional claim"
                            :handler (fn [opts claim]
                                       (let [[k v] (str/split claim #"=")]
                                         (update opts :claims assoc k v)))}]}
  [opts]
  {:data [(identity/create-user! (db) opts)]})

(defn update-user
  "Update a user/identity"
  {:flags ["--email <email>" {:doc "Email address"
                              :required true}
           "--password <password>" "User password"
           "--claim <k=v>" {:doc "Set a JWK claim, e.g. `--claim 'groups=admin owner'`

Leave the part after `=` blank to unset a claim."
                            :handler (fn [opts claim]
                                       (let [[k v] (str/split claim #"=")]
                                         (update opts :claims assoc k v)))}]}
  [{:keys [email password claims] :as opts}]
  (db/with-transaction [conn (db)]
    (if-let [{user-id :identifier/identity-id} (identifier/find-one conn {:type "email" :value email})]
      (do
        (when claims
          (let [{orig-claims :identity/claims} (identity/find-one conn {:id user-id})]
            (identity/update! conn {:id user-id :claims (reduce (fn [c [k v]]
                                                                  (if (str/blank? v)
                                                                    (dissoc c k)
                                                                    (assoc c k v)))
                                                                orig-claims
                                                                claims)})))
        (when password
          (credential/set-password! conn {:identity-id user-id :password password}))))))

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
    "update" #'update-user
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
     ((requiring-resolve 'co.gaiwan.oak.util.jose/build-jwt)
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
            (println ((requiring-resolve 'charred.api/write-json-str) (map #(update-keys % (comp (requiring-resolve 'camel-snake-kebab.core/->snake_case) name)) data)))

            (= "csv" (:format opts))
            (let [w (StringWriter.)
                  ks (distinct (mapcat keys data))]
              ((requiring-resolve 'charred.api/write-csv) w (cons ks
                                                                  (map (apply juxt (map (fn [col] #(get-col % col)) ks)) data))
               {:close-writer? true})
              (println (.toString w)))

            :else
            ((requiring-resolve 'clojure.pprint/print-table) (map first columns)
             (map (fn [row]
                    (into {} (map (fn [[h k]] [h (get-col row k)])) columns)) data)))))
      res)))

(defn run-cmd
  "Start the Oak IAM server"
  {:flags
   ["--port=<port>"
    {:doc "Set the HTTP port"
     :handler (fn [opts port]
                (swap! config/cli-opts assoc :http/port port)
                (config/reload!))}]}
  [_]
  (config/start!)
  @(promise))

(defn show-config-cmd
  "Give an overview of all configuration entries"
  [_]
  (config/load!)
  ((requiring-resolve 'clojure.pprint/print-table)
   (for [[k {:keys [val source]}] (config/entries)]
     {"key" k "value" val "source" source})))

(def commands
  ["run" #'run-cmd
   "show-config" #'show-config-cmd
   "jwk" jwk-commands
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
