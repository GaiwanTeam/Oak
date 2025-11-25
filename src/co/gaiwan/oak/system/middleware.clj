(ns co.gaiwan.oak.system.middleware
  "The middleware that executes before reitit matches the request
   https://cljdoc.org/d/metosin/reitit-ring/0.9.2/doc/ring/restful-form-methods
  "
  (:require
   [clojure.string :as str]
   [reitit.ring.middleware.parameters :as reitit-params]
   [reitit.ring.middleware.multipart :as reitit-multipart]))

(defn- hidden-method
  [request]
  (some-> (or (get-in request [:form-params "_method"])
              (get-in request [:multipart-params "_method"]))
          str/lower-case
          keyword))

(def wrap-hidden-method
  "Support the _method form field to mimic a non-POST request from a standard
  browser form."
  {:name ::wrap-hidden-method
   :wrap (fn [handler]
           (fn [request]
             (if-let [fm (and (= :post (:request-method request))
                              (hidden-method request))]
               (handler (assoc request :request-method fm))
               (handler request))))})

(defn component [_]
  [reitit-params/parameters-middleware
   reitit-multipart/multipart-middleware
   wrap-hidden-method])
