(ns co.gaiwan.oak.html.layout
  (:require
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.html.logo :as logo]
   [lambdaisland.ornament :as o]))

(require 'co.gaiwan.oak.html.styles)

(defn layout
  "For wrap-render"
  [{:html/keys [head body]}]
  [:html {:lang "en"}
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    (if (config/get :html/inline-css)
      [:style (o/defined-styles)]
      [:link {:rel "stylesheet" :type "text/css" :href "/styles.css"}])
    head]
   [:body
    body
    [:footer
     "Powered by"
     [logo/logo]]]])
