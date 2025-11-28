(ns co.gaiwan.oak.system.middleware
  "Global middleware, executes before reitit matches the request"
  (:require
   [clojure.string :as str]
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

(defn component [_]
  [reitit-params/parameters-middleware
   wrap-hidden-method])
