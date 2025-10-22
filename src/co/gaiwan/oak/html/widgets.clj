(ns co.gaiwan.oak.html.widgets
  "Generic components"
  (:require
   [co.gaiwan.oak.html.tokens :refer :all]
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

(o/defstyled full-center-card :div
  "Visual 'card' with rounded corners and drop shadow. Becomes seamless at
  smaller screen sizes. Intended to only have one on the screen."
  {:padding   --size-8
   :flex-grow 1
   :display          :flex
   :flex-direction   :column
   :align-items      :stretch
   :gap "1rem"}
  [:>* {:margin-top 0
        :margin-bottom 0}]
  [:at-media {:min-width "40rem"}
   {:max-width        --size-fluid-10
    :border-radius    --radius-3
    :box-shadow       --shadow-2
    :background-color --bg-panel
    :color            --text-panel}]
  [:h1
   {:text-align :center}])
