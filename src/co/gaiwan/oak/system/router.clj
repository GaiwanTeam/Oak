(ns co.gaiwan.oak.system.router
  "HTTP router and middleware setup"
  (:require
   [muuntaja.core :as muuntaja]
   [muuntaja.format.charred :as muuntaja-charred]
   [reitit.ring :as ring]
   [reitit.ring.middleware.muuntaja :as reitit-muuntaja]))

(defn routes []
  ["/ping" {:get {:handler (fn [req] {:body {:response "pong"}})}}])

(defn muuntaja-instance []
  (muuntaja/create
   (-> muuntaja/default-options
       (assoc-in [:formats "application/json"] muuntaja-charred/format)
       (assoc-in [:formats "application/json; charset=utf-8"] muuntaja-charred/format))))

(defn create-router [opts]
  (ring/router
   (routes)
   {:data {:muuntaja (muuntaja-instance)
           :middleware [reitit-muuntaja/format-negotiate-middleware
                        reitit-muuntaja/format-response-middleware
                        reitit-muuntaja/format-request-middleware]}}))

(def component
  {:start create-router})
