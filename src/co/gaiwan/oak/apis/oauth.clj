(ns co.gaiwan.oak.apis.oauth
  (:require
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]
   [co.gaiwan.oak.util.log :as log]
   [co.gaiwan.oak.util.routing :as routing]
   [honey.sql :as honey]
   [lambdaisland.hiccup.middleware :as hiccup-mw]
   [lambdaisland.uri :as uri]
   [next.jdbc :as jdbc]))

(defn execute! [db qry]
  (jdbc/execute! db (honey/format qry)))

(defn auth-error-html [extra-info]
  [:<>
   [:h1 "Oh no! Something went wrong."]
   [:hr]
   [:p "It looks like the link you clicked to sign in isn't quite right. Don't worry, this is usually a simple fix."]
   [:p "The application you're trying to use did not pass along all information needed to let you log in. This could be because of a broken link or a typo."]
   [:p "What you can do:"
    [:ul
     [:li "Head back to the application and try clicking the sign-in button again."]
     [:li "If the issue continues, please reach out to the application's support team. They'll know exactly what to do!"]]]
   [:p "We apologize for the inconvenience!"]
   [:hr]
   [:details
    [:summary "Technical information"]
    extra-info]])

(defn error-html-response [message]
  {:status 400
   :html/head [:title "Something went wrong"]
   :html/body [auth-error-html message]})

(defn error-redirect-response [redirect-uri error-type]
  {:status 302
   :headers {"Location" (uri/assoc-query redirect-uri :error error-type)}})

(defn GET-authorize
  {:summary "Authorize a client"
   :description "This endpoint is used to authorize a client to access a user's resources."
   :parameters {:query [:map
                        [:client_id {:optional true} string?]
                        [:redirect_uri {:optional true} string?]
                        [:response_type {:optional true} string?]
                        [:scope {:optional true} string?]
                        [:state {:optional true} string?]
                        [:code_challenge {:optional true} string?]
                        [:code_challenge_method {:optional true} string?]]}}
  [{:keys [db parameters session] :as req}]
  (let [{:keys [redirect_uri client_id response_type scope state code_challenge code_challenge_method]} (:query parameters)]
    (cond
      (not redirect_uri)
      [error-html-response "OAuth authorize redirect is missing the redirect_uri query parameter in the URL."]

      (not client_id)
      [error-html-response "OAuth authorize redirect is missing the client_id query parameter in the URL."]

      :else
      (let [oauth-client (oauth-client/find-by-client-id db client_id)]
        (cond
          ;; OAuth 2.1, match redirect_uri exactly, not just prefix
          (not (some #{redirect_uri} (:oauth-client/redirect-uris oauth-client)))
          [error-html-response "Invalid redirect_uri for this client"]

          (not response_type)
          (error-redirect-response redirect_uri "invalid_request")

          (not (some #{response_type} (:oauth-client/response-types oauth-client)))
          (error-redirect-response redirect_uri "unsupported_response_type")

          (not (:identity session))
          {:status 302
           :headers {"Location" (routing/path-for req :auth/login)}
           :session (assoc session :redirect-after-login (str (uri/assoc-query* (routing/path-for req :oauth/authorize)
                                                                                (:query parameters))))}

          :else
          {:status 200
           :html/body [:p "continue authorize"]}
          ))

      :else

      {:html/body [:p (pr-str parameters)]}))
  )

(def POST-register-client
  {:handler
   (fn [{:keys [db] :as req}]
     {:status 200})})

(defn component [opts]
  (log/info :message "Starting JWKS API")
  {:routes
   ["/oauth" {:openapi {:tags ["oauth"]}}
    ["/authorize" {:name :oauth/authorize
                   :middleware [hiccup-mw/wrap-render]
                   :get #'GET-authorize}]
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
