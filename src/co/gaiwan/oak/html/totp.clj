(ns co.gaiwan.oak.html.totp
  (:require
   [lambdaisland.ornament :as o]))

(defn setup-page [{:keys [data-uri next-uri]}]
  [:p "Set up TOTP here!"
   [:img {:src data-uri}]
   [:a {:href next-uri} "Continue"]])

(defn verify-failed-page [{:keys [next-uri]}]
  [:div "Invalid code. Please check and re-enter."
   [:a {:href next-uri} "Continue"]])

(defn verify-success-page [{:keys [cred-save-success?]}]
  (if cred-save-success?
    [:div "Your authenticator device has been successfully linked."]
    [:div "Encountering error when recording credentials"]))
