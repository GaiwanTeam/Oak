(ns co.gaiwan.oak.apis.oauth
  (:require
   [co.gaiwan.oak.util.log :as log]
   [honey.sql :as honey]
   [next.jdbc :as jdbc]))

(defn execute! [db qry]
  (jdbc/execute! db (honey/format qry)))

(def GET-authorize
  {:summary "Authorize a client"
   :description "This endpoint is used to authorize a client to access a user's resources."
   :parameters {:query [:map
                        [:response_type {:optional true} string?]
                        [:client_id {:optional true} string?]
                        [:redirect_uri {:optional true} string?]
                        [:scope {:optional true} string?]
                        [:state {:optional true} string?]
                        [:code_challenge {:optional true} string?]
                        [:code_challenge_method {:optional true} string?]]}
   :handler (fn [{:keys [db parameters] :as req}]
              {:status 200
               :body {:GOT parameters}})})

(def POST-register-client
  {:handler
   (fn [{:keys [db] :as req}]
     {:status 200})})

(defn component [opts]
  (log/info :message "Starting JWKS API")
  {:routes
   ["/oauth" {:openapi {:tags ["oauth"]}}

    ["/authorize" {:get GET-authorize}]
    #_["/token" {:post {}}
       [""]
       ["/revoke" {:post {}}]]

    #_["/userinfo" {:get {}}]

    #_["/client" {}
       ["/register"
        ["" {:post {:handler #'POST-register-client}}]
        ["/:client-id" {:get {}
                        :put {}
                        :delete {}}]]]
    ]})

(comment
  (user/restart! :apis/oauth))
