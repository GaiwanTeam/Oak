(ns co.gaiwan.oak.html.layout
  (:require
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.html.forms :as forms]
   [co.gaiwan.oak.html.graphics :as g]
   [co.gaiwan.oak.html.tokens :as t]
   [lambdaisland.ornament :as o]))

(require 'co.gaiwan.oak.html.styles)

(defn layout
  "For wrap-render"
  [{:html/keys [head body]}]
  [:html {:lang "en"}
   [:head
    [:link {:rel "icon" :href "/favicon.ico"}]
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:script {:type "module" :src "/scripts/oak.js"}]
    (if (config/get :html/inline-css)
      [:style (o/defined-styles)]
      [:link {:rel "stylesheet" :type "text/css" :href "/styles.css"}])
    head]
   [:body
    body
    [:footer
     "Powered by"
     [g/logo]]
    [:template#password-visibility-toggle
     [forms/visibility-toggle]]]])

(o/defstyled flash-box :div
  {:background-color t/--bg-panel
   :padding t/--size-2
   :margin-top t/--size-1
   :margin-bottom t/--size-2
   :border-radius t/--radius-3
   :box-shadow t/--shadow-2
   :font-size "110%"
   :font-weight :bold
   :color t/--text-panel}
  [:button {:float "right"}]
  ([props message]
   [:<>
    props
    message
    [:button.ghost {:oak-remove "parent"} "close"]]))

(defn flash-error [msg]
  [flash-box
   {:style
    {t/--bg-panel t/--red-2
     t/--text-panel t/--red-12}} msg])

(defn flash-success [msg]
  [flash-box
   {:style {t/--bg-panel t/--oak-green-2
            t/--text-panel t/--oak-green-13}}
   msg])
