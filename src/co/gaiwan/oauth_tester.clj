(ns co.gaiwan.oauth-tester
  "OAuth server implementation tester

  Command line interface for testing OAuth server implementations.
  "
  (:require
   [clojure.string :as str]
   [clojure.pprint :as pprint]
   [lambdaisland.cli :as cli]
   [co.gaiwan.oauth-tester.impl :as tester]))

(set! *print-namespace-maps* false)

(def init {})

(defn test-oauth-server
  "Test an OAuth server implementation"
  {:flags ["--server-url <url>" {:doc "Base URL of the OAuth server to test"
                                 :required true}
           "--client-id <id>" {:doc "OAuth client ID"
                               :required true}
           "--client-secret <secret>" {:doc "OAuth client secret"
                                       :required true}
           "--authorization-endpoint <path>" {:doc "Path to the authorization endpoint"}
           "--redirect-uri <uri>" {:doc "Redirect URI for authorization flow"
                                   :required true}
           "--scope <scope>" {:doc "Scope to request"
                              :default "openid"}]}
  [opts]
  (tester/run-full-suite
   opts
   (fn [ctx res]
     (case (:result res)
       :ok
       (println "- ✅" (:doc res))
       :warn
       (do
         (println "- ⚠" (:doc res))
         (println "    " (:message res)))
       :fail
       (do
         (println "- ❌" (:doc res))
         (println "    " (:message res))
         (println "    " (:description res)))))))

(def commands ["test" #'test-oauth-server])

(def flags
  ["--show-trace" "Show full stack trace in errors"
   "-v, --verbose" "Increase verbosity"
   "-h, --help" "Show help text for a (sub-)command"])

(defn wrap-error [handler]
  (fn [opts]
    (try
      (handler opts)
      (catch clojure.lang.ExceptionInfo e
        (let [d (ex-data e)]
          (if (= :lambdaisland.cli/parse-error (:type d))
            (println (ex-message e))
            (do
              (println "Error:" (ex-message e))
              (println "Use --show-trace to see the full error trace.")))))
      (catch Throwable t
        (println "Error:" (ex-message t))
        (println "Use --show-trace to see the full error trace.")))))

(defn -main [& args]
  (cli/dispatch*
   {:name "oauth-tester"
    :init init
    :flags flags
    :commands commands
    :middleware [wrap-error]}
   args))

(comment
  (-main "test" "--help")
  (-main "test" "--server-url" "https://example.com" "--client-id" "test" "--client-secret" "secret" "--redirect-uri" "https://example.com/callback"))
