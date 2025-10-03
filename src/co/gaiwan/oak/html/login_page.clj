(ns co.gaiwan.oak.html.login-page
  (:require
   [co.gaiwan.oak.html.tokens :refer :all]
   [co.gaiwan.oak.lib.form :as form]
   [lambdaisland.ornament :as o]))

(o/defstyled leaf-bg :svg
  {:position "absolute"
   :top "0"
   :left "0"
   :z-index -1
   :width --size-fluid-8}
  ([]
   [:<> {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 19.866 12.056"}
    [:path {:d "M-1174.322-2194.93h-15.138c-.262.884-.475 1.85-1.17 1.847-.577 0-.368-.904-1.108-.906-.48 0-.513.245-.88.562v6.332c.288.624.745 1.266 1.468 1.122.6-.121 1.251-.344 1.351-.927.09-.532-.167-1.527.052-1.564.709-.121 1.724 6.996 5.252 5.34.38-.179.558-.527.672-.876.171-.53-.428-1.577-.05-1.67.221-.054 4.921 3.395 6.976.918 1.492-1.799-.865-2.021-.241-2.754.265-.311 1.135-.188 1.835-.335.823-.173 1.567-.48 1.855-1.123.508-1.13-.23-1.48-.12-1.946.112-.464.813-1.3.39-2.369s-.738-.795-.971-1.18c-.108-.176-.122-.327-.172-.471z"
            :transform "translate(1192.91 2194.93)"
            :fill "#72a182"}]
    [:path {:d "M-1178.196-2194.93h-14.714v8.623c.772 1.355 1.852 2.395 3.445 1.648.38-.179.559-.526.672-.876.171-.53-.427-1.577-.05-1.67.222-.054 4.921 3.395 6.976.918 1.492-1.799-.865-2.022-.241-2.754.265-.312 1.136-.188 1.835-.335.823-.174 1.567-.479 1.855-1.123.507-1.13-.23-1.481-.119-1.946s.812-1.299.389-2.369z"
            :transform "translate(1192.91 2194.93)"
            :fill "#4e765c"}]]))

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

(o/defstyled submit :input.call-to-action
  {:width            "100%"
   :padding          --size-2
   :border           :none
   :border-radius    --radius-2
   :font-size        "1rem"
   :font-weight      :bold
   :cursor           :pointer})

(o/defstyled login-html :main
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
  [:h1
   {:text-align    :center
    :margin-top    0
    :margin-bottom --size-6
    }]
  ([]
   [:<>
    [leaf-bg]
    [:div
     [:h1 "Login"]
     [form/form {:method "POST"}
      [input-group
       {:label            "Email address"
        :id               "email"
        :type             "email"
        :name             "email"
        :required         "required"
        :autocomplete     "username"
        :placeholder      "you@example.com"
        :aria-describedby "email-help"}]
      [input-group
       {:id           "password"
        :label        "Password"
        :type         "password"
        :name         "password"
        :required     "required"
        :autocomplete "current-password"
        :placeholder  "Enter your password"}]
      [submit {:type "submit" :value "Log In"}]]]]))
