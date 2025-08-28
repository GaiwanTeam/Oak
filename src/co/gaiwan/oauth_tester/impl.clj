(ns co.gaiwan.oauth-tester.impl
  "OAuth tester tool implementation"
  (:require
   [charred.api :as charred]
   [clojure.string :as str]
   [hato.client :as hato]
   [lambdaisland.uri :as uri]))

(require 'co.gaiwan.oak.lib.hato-charred)

(defn fetch [ctx req]
  (hato/request (cond-> (merge {:as :auto
                                :throw-exceptions? false} req)
                  (not (:request-method req))
                  (assoc :request-method :get)
                  (and (:server-url ctx) (:path req))
                  (assoc :url (-> (str (:server-url ctx)
                                       (:path req))
                                  #_   (uri/assoc-query* (:query-params req))
                                  str))
                  #_#_:->
                  (assoc-in [:headers "host"]
                            (str
                             (assoc (uri/uri (:server-url ctx))
                                    :path nil :query nil))))))

(defn fetch-rfc8414-metadata
  "Retrieve OAuth server configuration using .well-known endpoint

  RFC 8414 - OAuth 2.0 Authorization Server Metadata
  "
  [ctx]
  (fetch ctx {:path "/.well-known/oauth-authorization-server"}))

(comment
  (def oak-url "http://localhost:4800")
  (def kc-url "http://localhost:8080/realms/master")
  (fetch-rfc8414-metadata {:server-url oak-url})
  (fetch-rfc8414-metadata kc-url)
  )

(defn authorization-endpoint-rejects-implicit-grant
  {:doc "The server rejects requests with `response_type=token`"
   :description "The Implicit Grant is no longer considered secure, and is being removed in OAuth 2.1."
   :categories #{:authorization-endpoint}
   :references ["hatos://datatracker.ietf.org/doc/html/rfc9700#name-implicit-grant"]
   :severity :warn}
  [ctx]
  (let [{:keys [status headers body]}
        (fetch ctx {:path (:authorization-endpoint ctx)
                    :query-params
                    {:client_id (:client-id ctx)
                     :client_secret (:client-secret ctx)
                     :redirect_uri (:redirect-uri ctx)
                     :response_type "token"
                     :scope ""
                     :state "123"
                     :code_challenge "xxx"
                     :code_challenge_method "S256"}})]
    (if (= 302 status)
      (let [location (uri/uri (get headers "location"))
            query-map  (uri/query-map location)
            fragment-map (uri/query-string->map (:fragment location))
            error (or (:error query-map) (:error fragment-map))]
        (cond
          (#{"unauthorized_client" "unsupported_response_type"} error)
          {:result :ok}
          error
          {:result :warn
           :message (str "Expected error=unauthorized_client or error=unauthorized_client, got" error)}
          :else
          {:result :fail
           :message (str "Expected error in query or fragment, got " location)}))
      {:result :fail
       :message (str "Expected 302 with error param, got " status)})))

(def all-checks
  [#'authorization-endpoint-rejects-implicit-grant])

(defn run-check [ctx check]
  (let [res (check ctx)]
    (merge (meta check) res)))

(defn run-full-suite
  ([ctx]
   (run-full-suite ctx nil))
  ([ctx report-fn]
   (reduce
    (fn [{:keys [checks ctx]} check]
      (let [res (run-check ctx check)
            ctx (or (:ctx res) ctx)]
        (when report-fn
          (report-fn ctx res))
        {:checks (conj checks res)
         :ctx ctx}))
    {:checks [] :ctx ctx} all-checks)))

(comment
  (run-full-suite {:server-url "hato://localhost:4800"
                   :client-id "63c0Q8dnkLD.oak.client"
                   :client-secret "2BFTnYcb8FYmDBhCurZ0o25dhD6oaSQy6"
                   :authorization-endpoint "/oauth/authorize"
                   :redirect-uri "https://example.com/redirect"
                   :scope ""}))
