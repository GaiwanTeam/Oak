(ns co.gaiwan.oak.html.forms
  "Form components"
  (:require
   [clojure.string :as str]
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.html.graphics :as g]
   [co.gaiwan.oak.html.tokens :refer :all]
   [co.gaiwan.oak.lib.ring-csp :as csp]
   [lambdaisland.ornament :as o]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(defn form
  "Drop in replacement for :form, but adds a anti-forgery-token field"
  [props & children]
  (into
   [:form props
    [:input {:type "hidden"
             :id "__anti-forgery-token"
             :name "__anti-forgery-token"
             :value
             (-> *anti-forgery-token*
                 (str/replace "&" "&amp;")
                 (str/replace "\"" "&quot;")
                 (str/replace "<" "&lt;"))}]]
   children))

(o/defstyled input-group :div
  {:margin-bottom --size-4
   :position "relative"}
  ["&:has([type=hidden])"
   {:display       "none"}]
  [:label
   {:display       "block"
    :margin-bottom --size-2
    :font-weight   600}]
  [:input
   {:width "100%"}
   [:&:focus
    {:outline :none
     :border-color --oak-green-5}]
   ["&[aria-invalid='true']" {:box-shadow (str "0 0 0 3px " --bg-panel ", 0 0 0 5px " --status-error)}]]
  [:.error {:color   --status-error
            :margin-top "0.5rem"
            :font-weight 600}]
  [".input-wrap:not(:has([aria-invalid='true'])) + .error" {:display "none"}]
  [g/circle-bang {:display       :inline-block
                  :width         "1em"
                  :height        "1em"
                  :margin-right  "0.3em"
                  :margin-bottom "-0.1em"}]
  [:.input-wrap {:position "relative"}]
  ([props]
   [:<>
    [:label {:for (:id props)} (:label props)]
    [:div.input-wrap
     [:input (cond-> (assoc (dissoc props :error) :aria-describedby (str (:id props) "-error"))
               (:error props)
               (assoc :aria-invalid true))]]
    [:div.error {:id (str (:id props) "-error")} [g/circle-bang] (:error props)]]))

(o/defstyled submit-disable :input
  {:width            "100%"})

(o/defstyled submit-delete :input.cautious-action.severe
  {:width            "100%"})

(o/defstyled submit :input.call-to-action
  {:width            "100%"})

(o/defstyled visibility-toggle :button
  {:position "absolute"
   :border "none"
   :background-color "transparent"
   :right "0.5rem"
   :top "50%"
   :transform "translateY(-50%)"}
  ["&:hover" {:background-color "transparent"}]
  ["&[aria-pressed='false']" [g/eye {:display "block"}]]
  ["&[aria-pressed='true']" [g/eye-closed {:display "block"}]]
  [:svg {:display "none" :height "2em" :width "2em"}]
  ([]
   [:<> {:type "button" :aria-pressed "false"}
    [g/eye]
    [g/eye-closed]]))
