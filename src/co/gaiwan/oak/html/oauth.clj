(ns co.gaiwan.oak.html.oauth
  (:require
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.domain.scope :as scope]
   [co.gaiwan.oak.html.forms :as f]
   [co.gaiwan.oak.html.widgets :as w]
   [lambdaisland.ornament :as o]))

(o/defstyled layout :main
  {:display         :flex
   :justify-content :center
   :align-items     :center
   :min-height      "100vh"}
  ([& children]
   (into
    [:<> [w/leaf-bg]]
    children)))

(defn auth-error-html [extra-info]
  [layout
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

(o/defstyled submit-buttons :div
  {:display "flex"
   :gap "1rem"
   :width "100%"}
  [:>* {:flex-grow 1}]
  ([]
   [:<>
    [:button.call-to-action {:type "submit", :name "allow", :value "true"} "Allow"]
    [:button {:type "submit", :name "allow", :value "false"} "Cancel"]]))

(defn permission-dialog-html [oauth-client requested-scopes params]
  (let [{:keys [client_id redirect_uri response_type scope state code_challenge code_challenge_method]} params]
    [layout
     [w/full-center-card
      [:h1 (:oauth-client/client-name oauth-client) " Wants to Use Your " (config/get :org/name) " Account"]
      [:p "This will allow " (:oauth-client/client-name oauth-client) " to:"]
      [:ul
       (for [s requested-scopes]
         [:li (scope/desc s)])]
      [f/form {:method "post"}
       [:input {:type "hidden", :name "client_id", :value client_id}]
       [:input {:type "hidden", :name "redirect_uri", :value redirect_uri}]
       [:input {:type "hidden", :name "response_type", :value response_type}]
       [:input {:type "hidden", :name "scope", :value scope}]
       [:input {:type "hidden", :name "code_challenge", :value code_challenge}]
       [:input {:type "hidden", :name "code_challenge_method", :value code_challenge_method}]
       (when state [:input {:type "hidden", :name "state", :value state}])
       [submit-buttons]]]]))
