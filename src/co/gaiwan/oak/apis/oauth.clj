(ns co.gaiwan.oak.apis.oauth
  (:require
   [co.gaiwan.oak.util.log :as log]
   [honey.sql :as honey]
   [next.jdbc :as jdbc]
   [lambdaisland.hiccup.middleware :as hiccup-mw]))

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
  [{:keys [db parameters] :as req}]
  (let [{:keys [redirect_uri client_id response_type scope state code_challenge code_challenge_method]} (:query parameters)]
    (cond
      (not redirect_uri)
      {:status 400
       :html/head [:title "Something went wrong"]
       :html/body [auth-error-html "OAuth authorize redirect is missing the redirect_uri query parameter in the URL."]}

      (not client_id)
      {:status 400
       :html/head [:title "Something went wrong"]
       :html/body [auth-error-html "OAuth authorize redirect is missing the client_id query parameter in the URL."]}

      ;; :else
      ;; (some nil? [response_type scope state code_challenge code_challenge_method])
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

    ["/authorize" {:middleware [hiccup-mw/wrap-render]
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
