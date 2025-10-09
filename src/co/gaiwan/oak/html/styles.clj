(ns co.gaiwan.oak.html.styles
  "Global CSS declarations"
  (:require
   [co.gaiwan.oak.html.tokens :refer :all]
   [lambdaisland.ornament :as o]))

(o/defrules base-styles
  [#{"*" "*::before" "*::after"}
   {:box-sizing "border-box"}]
  [:body
   {:margin           0
    :font-family      --font-system-ui
    :background-color --bg-surface
    :color            --text-surface
    :min-height       "100vh"}]

  [:footer
   {:position :absolute
    :bottom "1em"
    :right "1em"}]

  [#{"input[type=\"email\"]"
     "input[type=\"password\"]"}
   {:border-width  "1px"
    :border-style  "solid"
    :border-color  --border-input
    :border-radius --radius-2
    :padding       --size-2
    :font-size     "1rem"}]

  [:.call-to-action
   {:background-color --bg-call-to-action
    :color            --text-call-to-action
    :transition       "background-color 0.2s ease"}
   [:&:hover
    {:background-color --bg-call-to-action-hover}]])
