(ns co.gaiwan.oak.html.totp
  (:require
   [co.gaiwan.oak.html.tokens :refer :all]
   [co.gaiwan.oak.lib.form :as form]
   [lambdaisland.ornament :as o]))

(defn verify-failed-page [{:keys [next-uri]}]
  [:div "Invalid code. Please check and re-enter."
   [:a {:href next-uri} "Continue"]])

(defn verify-success-page [{:keys [cred-save-success?]}]
  (if cred-save-success?
    [:div "Your authenticator device has been successfully linked."]
    [:div "Encountering error when recording credentials"]))

(comment
  (defn setup-page [{:keys [data-uri next-uri]}]
    [:p "Set up TOTP here!"
     [:img {:src data-uri}]
     [:a {:href next-uri} "Continue"]]))

(o/defstyled next-link :a.call-to-action
  {:width            "100%"
   :padding          --size-2
   :border           :none
   :border-radius    --radius-2
   :font-size        "1rem"
   :font-weight      :bold
   :cursor           :pointer})

(o/defstyled next-button :input.call-to-action
  {:width            "100%"
   :padding          --size-2
   :border           :none
   :border-radius    --radius-2
   :font-size        "1rem"
   :font-weight      :bold
   :cursor           :pointer})

(o/defstyled setup-page :main
  {:display         :flex
   :justify-content :center
   :align-items     :center
   :min-height      "100vh"}
  [:>div
   {:margin    "0 auto"
    :padding   --size-8
    :flex-grow 1}]
  [:at-media {:min-width "40rem"}
   [:>div
    {:display          :flex
     :flex-direction   :column
     :max-width        --size-fluid-10
     :flex-grow        1
     :border-radius    --radius-3
     :box-shadow       --shadow-2
     :background-color --bg-panel
     :color            --text-panel}]]
  ([{:keys [data-uri next-uri]}]
   [:div
    [:p "Set up TOTP here!"]
    [:img {:src data-uri}]
    [next-link
     {:href next-uri} "Continue"]]))

(comment
  (defn verify-form []
    [form/form {:method "POST"}
     [:input {:type "text" :name "code"}]
     [:input {:type "submit" :value "Verify 2FA Setup"}]]))

(o/defstyled input-group :div
  {:margin-bottom --size-4}
  [:label
   {:display       "block"
    :margin-bottom --size-2
    :font-weight   :bold}]
  [:input
   {:width "100%"}]
  ([props]
   [:<>
    [:label {:for (:id props)} (:label props)]
    [:input props]]))

(o/defstyled verify-form :main
  {:display         :flex
   :justify-content :center
   :align-items     :center
   :min-height      "100vh"}
  [:>div
   {:margin    "0 auto"
    :padding   --size-8
    :flex-grow 1}]
  [:at-media {:min-width "40rem"}
   [:>div
    {:display          :flex
     :flex-direction   :column
     :max-width        --size-fluid-10
     :flex-grow        1
     :border-radius    --radius-3
     :box-shadow       --shadow-2
     :background-color --bg-panel
     :color            --text-panel}]]
  ([]
   [:<>
    [:div
     [form/form {:method "POST"}
      [input-group
       {:label            "2FA code from Authenticator"
        :id               "code"
        :type             "text"
        :name             "code"
        :required         "required"
        :aria-describedby "2FA code"}]
      [next-button {:type "submit" :value "Verify 2FA Setup"}]]]]))
