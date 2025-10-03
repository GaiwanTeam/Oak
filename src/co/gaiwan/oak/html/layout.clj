(ns co.gaiwan.oak.html.layout
  (:require
   [co.gaiwan.oak.html.logo :as logo]
   [lambdaisland.ornament :as o]))

(o/defrules global-styles
  [:footer
   {:position :absolute
    :bottom "1em"
    :right "1em"
    }]
  )

(defn layout
  "For wrap-render"
  [{:html/keys [head body]}]
  [:html {:lang "en"}
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:style (o/defined-styles)]
    head]
   [:body
    body
    [:footer
     "Powered by"
     [logo/logo]]]])
