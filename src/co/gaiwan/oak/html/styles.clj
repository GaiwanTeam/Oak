(ns co.gaiwan.oak.html.styles
  "Global CSS declarations"
  (:require
   [co.gaiwan.oak.html.tokens :refer :all]
   [lambdaisland.ornament :as o]))

(o/defrules base-styles
  [#{"*" "*::before" "*::after"}
   {:box-sizing "border-box"}]

  ["@view-transition" {:navigation :auto}]

  [:body
   {:margin           0
    :font-family      --font-system-ui
    :background-color --bg-surface
    :color            --text-surface
    :min-height       "100vh"}]

  [:footer
   {:position :absolute
    :bottom   "1em"
    :right    "1em"
    :z-index  "-1"
    :color    --text-subtle}]

  [#{"input[type=\"email\"]"
     "input[type=\"password\"]"
     "input[type=\"text\"]"}
   {:border-width  "1px"
    :border-style  "solid"
    :border-color  --border-input
    :border-radius --radius-2
    :padding       --size-2
    :font-size     "1rem"}]

  [#{:button "input[type=\"submit\"]"}
   {:background-color --button-color
    :border           (str "1px solid " --button-border)
    :color            --text-button
    :padding          --size-2
    :border-radius    --radius-2
    :font-size        "1rem"
    :font-weight      :bold
    :cursor           :pointer}
   [:&:hover
    {:background-color --button-color-hover}]
   [:&:active
    {:background-image --button-color-dark}]]

  [:button.cautious-action
   {:background-color --white
    :border           (str "2px solid " --gray-4)
    :color            --gray-6
    :padding          --size-2
    :border-radius    --radius-2
    :font-size        "1rem"
    :font-weight      :bold
    :cursor           :pointer}
   [:&:hover
    {:border-color --gray-5
     :color --gray-7
     :background-color --gray-0}]
   [:&:active
    {:border-color --gray-6
     :color --gray-8
     :background-color --gray-1}]]
  [:button.cautious-action.severe
   [:&:hover
    {:border-color --red-5
     :color --red-7
     :background-color --red-0}]]
  [:a.subtle
   {:color --text-subtle}]

  [:div.status-indicator
   {:border-radius "50%"
    :width "0.75rem"
    :height "0.75rem"}]
  [:div.status-active
   {:background-color --status-info}]
  [:.call-to-action
   {--button-color       --bg-call-to-action
    --button-color-light --bg-call-to-action-light
    --button-color-dark  --bg-call-to-action-dark
    --button-color-hover --bg-call-to-action-hover
    --button-border      --border-call-to-action
    --text-button        --text-call-to-action
    :background-image    (str "linear-gradient(to bottom, "
                              --button-color-light  " 0%, "
                              --button-color " 100%)")
    :box-shadow          "/* Inner shadow for slight bevel/highlight effect */
   inset 0 1px 0 rgba(255, 255, 255, 0.4),
   /* Subtle outer shadow for lift */
   0 4px 6px rgba(0, 0, 0, 0.2),
   /* Darker shadow at the bottom for more depth */
   0 1px 3px rgba(0, 0, 0, 0.4)"
    :transition          "background-color .1s ease, box-shadow .1s ease"}
   [:&:hover
    {:background-image (str "linear-gradient(to top, "
                            --button-color-light " 0%, "
                            --button-color " 100%)")
     :box-shadow       "inset 0 1px 0 rgba(255, 255, 255, 0.6),
     0 6px 10px rgba(0, 0, 0, 0.3), /* Slightly larger shadow on hover */
     0 1px 5px rgba(0, 0, 0, 0.5)"}]

   [:&:active
    {:box-shadow "inset 0 1px 3px rgba(0, 0, 0, 0.6), /* Strong inner shadow */
    0 1px 1px rgba(0, 0, 0, 0.1) /* Very small outer shadow */"
     :transform  "translateY(1px)"}]])
