(ns co.gaiwan.oak.html.forms
  "Form components"
  (:require
   [clojure.string :as str]
   [co.gaiwan.oak.html.tokens :refer :all]
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
  {:margin-bottom --size-4}
  [:label
   {:display       "block"
    :margin-bottom --size-2
    :font-weight   600}]
  [:input
   {:width "100%"}
   [:&:focus
    {:outline :none
     :border-color --oak-green-5}]]
  ([props]
   [:<>
    [:label {:for (:id props)} (:label props)]
    [:input props]]))

(o/defstyled submit :input.call-to-action
  {:width            "100%"
   :padding          --size-2
   :border-radius    --radius-2
   :font-size        "1rem"
   :font-weight      :bold
   :cursor           :pointer})
