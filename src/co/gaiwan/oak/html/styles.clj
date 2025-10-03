(ns co.gaiwan.oak.html.styles
  (:require
   [co.gaiwan.oak.html.tokens :refer :all]
   [lambdaisland.ornament :as o]))

(o/defrules base-styles
  [:body
   {:font-family      --font-system-ui
    :background-color --surface-bg
    :color            --surface-text
    :min-height       "100vh"}]
  )
