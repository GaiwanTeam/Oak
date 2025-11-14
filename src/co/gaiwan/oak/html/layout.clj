(ns co.gaiwan.oak.html.layout
  (:require
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.html.graphics :as g]
   [co.gaiwan.oak.lib.ring-csp :as csp]
   [lambdaisland.ornament :as o]))

(require 'co.gaiwan.oak.html.styles)

(defn bottom-script []
  [:script {:nonce (str csp/*csp-nonce*)}
   "document.querySelectorAll(\" [data-confirm] \").forEach((el)=>el.onclick=()=>confirm(el.dataset.confirm))"])

(defn layout
  "For wrap-render"
  [{:html/keys [head body]}]
  [:html {:lang "en"}
   [:head
    [:link {:rel "icon" :href "/favicon.ico"}]
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
     [g/logo]]
    [bottom-script]]])
