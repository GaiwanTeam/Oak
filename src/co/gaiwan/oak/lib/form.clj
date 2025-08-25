(ns co.gaiwan.oak.lib.form
  "Hiccup form components, injects the CSRF token"
  (:require
   [clojure.string :as str]
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
             (-> (force *anti-forgery-token*)
                 (str/replace "&" "&amp;")
                 (str/replace "\"" "&quot;")
                 (str/replace "<" "&lt;"))}]]
   children))
