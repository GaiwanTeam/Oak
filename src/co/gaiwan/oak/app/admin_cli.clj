(ns co.gaiwan.oak.app.admin-cli
  (:require
   [charred.api :as json]
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.domain.jwk :as jwk]
   [lambdaisland.cli :as cli]
   [clojure.pprint :as pprint])
  (:import java.io.StringWriter))

(def init {})

(defn db []
  ;; placeholder, eventually this needs to be tenant-aware
  (config/start! [:system/database])
  (:data-source (config/component :system/database)))

(defn create-jwk
  "Create a new JWK"
  {:flags ["--kty <kty>" {:doc "Key type, e.g. RSA, OKP, EC"
                          :default "OKP"}
           "--alg <alg>" {:doc "Algorithm used, must match key type."}
           "--crv <crv>" {:doc "Curve used, for EC or OKP keys"}
           "--size <size>" {:doc "Key size, for RSA keys"
                            :parse parse-long}]}
  [opts]
  (jwk/create! (db) (update-keys opts name))
  )

(defn list-jwk
  "List JWKs"
  [opts]
  {:columns [["Type" "kty"]
             ["Id" "kid"]]
   :data (jwk/list-all (db))})

(def jwk-commands {:doc      "Read and manipulate JWKs"
                   :commands ["create" #'create-jwk
                              "list" #'list-jwk]})

(def commands ["jwk" jwk-commands])

(def flags
  ["-v, --verbose" "Increase verbosity"
   "-h, --help" "Show help text for a (sub-)command"
   "--format <json|csv>" "Output JSON/CSV rather than human-readable data"])

(defn wrap-stop-system [handler]
  (fn [opts]
    (let [res (handler opts)]
      (config/stop!)
      res)))

(defn wrap-print-output [handler]
  (fn [opts]
    (let [{:keys [data columns] :as res} (handler opts)]
      (when data
        (cond
          (= "json" (:format opts))
          (println (json/write-json-str data))

          (= "csv" (:format opts))
          (let [w (StringWriter.)
                ks (distinct (mapcat keys data))]
            (json/write-csv w (cons ks
                                    (map (apply juxt (map (fn [col] #(get % col)) ks)) data))
                            {:close-writer? true})
            (println (.toString w)))

          :else
          (pprint/print-table (map first columns)
                              (map (fn [row]
                                     (into {} (map (fn [[h k]] [h (get row k)])) columns)) data))))
      res)
    ))

(defn -main [& args]
  (cli/dispatch*
   {:name "oakadm"
    :init init
    :flags flags
    :commands commands
    :middleware [wrap-stop-system wrap-print-output]}
   args))

;; Local Variables:
;; mode:clojure
;; End:
