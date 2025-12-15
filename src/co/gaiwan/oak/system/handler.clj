(ns co.gaiwan.oak.system.handler
  "HTTP handler, Ring-based"
  (:require
   [clojure.string :as str]
   [co.gaiwan.oak.lib.auth-middleware :as auth-mw]
   [reitit.ring :as reitit-ring]
   [reitit.ring.middleware.parameters :as reitit-params]))

(defn- hidden-method
  [request]
  (some-> (get-in request [:form-params "_method"])
          str/lower-case
          keyword))

(def wrap-hidden-method
  "Support the _method form field to mimic a non-POST request from a standard
  browser form.

  See https://cljdoc.org/d/metosin/reitit-ring/0.9.2/doc/ring/restful-form-methods"
  {:name ::wrap-hidden-method
   :wrap (fn [handler]
           (fn [request]
             (if-let [fm (and (= :post (:request-method request))
                              (hidden-method request))]
               (handler (-> request
                            (assoc :request-method fm)
                            (update :form-params dissoc "_method")))
               (handler request))))})

(defn component [config]
  (reitit-ring/ring-handler
   (:router config)
   (reitit-ring/create-default-handler)
   {:middleware [reitit-params/parameters-middleware
                 wrap-hidden-method]}))
