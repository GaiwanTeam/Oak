(ns co.gaiwan.oak.html.auth
  (:require
   [co.gaiwan.oak.html.tokens :refer :all]
   [co.gaiwan.oak.lib.form :as form]
   [lambdaisland.ornament :as o]))

(o/defstyled main-layout :main
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
     :justify-content  :center
     :align-items      :center
     :flex-direction   :column
     :max-width        --size-fluid-10
     :flex-grow        1
     :border-radius    --radius-3
     :box-shadow       --shadow-2
     :background-color --bg-panel
     :color            --text-panel}]]
  ([content]
   content))

(defn message [text]
  [main-layout
   [:div
    [:p
     text]]])


