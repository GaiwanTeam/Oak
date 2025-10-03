(ns co.gaiwan.oauth-tester
  "OAuth server implementation tester

  Command line interface for testing OAuth server implementations.
  "
  (:require
   [co.gaiwan.oak.lib.cli-error-mw :as cli-error-mw]
   [co.gaiwan.oauth-tester.impl :as tester]
   [lambdaisland.cli :as cli]))

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

(defn -main [& args]
  (cli/dispatch*
   {:name "oauth-tester"
    :init init
    :flags flags
    :commands commands
    :middleware [cli-error-mw/wrap-error]}
   args))

(comment
  (-main "test" "--help")
  (-main "test" "--server-url" "https://example.com" "--client-id" "test" "--client-secret" "secret" "--redirect-uri" "https://example.com/callback"))
