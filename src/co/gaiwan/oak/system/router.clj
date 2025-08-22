(ns co.gaiwan.oak.system.router
  "HTTP router and middleware setup"
  (:require
   [co.gaiwan.oak.util.log :as log]
   [muuntaja.core :as muuntaja]
   [muuntaja.format.charred :as muuntaja-charred]
   [reitit.coercion :as coercion]
   [reitit.coercion.malli]
   [reitit.openapi :as reitit-openapi]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as ring-coercion]
   [reitit.ring.middleware.muuntaja :as reitit-muuntaja]
   [reitit.ring.middleware.parameters :as reitit-params]))

(defn muuntaja-instance []
  (muuntaja/create
   (-> muuntaja/default-options
       (assoc-in [:formats "application/json"] muuntaja-charred/format)
       (assoc-in [:formats "application/json; charset=utf-8"] muuntaja-charred/format))))

(defn wrap-request-filter [handler rf]
  (fn [req]
    (handler (rf req))))

(def coercion-options
  {:error-keys #{:type :coercion :in :schema :value :errors :humanized :transformed}})

(defn component [{:keys [routes request-filters]}]
  (let [request-filter (apply comp (keep :http/request-filter request-filters))
        routes         (into ["" {}] (map :routes routes))]
    (log/info :routes routes)
    (ring/router
     routes
     {:data {:coercion   (reitit.coercion.malli/create coercion-options)
             :compile    coercion/compile-request-coercers
             :muuntaja   (muuntaja-instance)
             :middleware [reitit-openapi/openapi-feature
                          reitit-params/parameters-middleware
                          reitit-muuntaja/format-negotiate-middleware
                          reitit-muuntaja/format-response-middleware
                          reitit-muuntaja/format-request-middleware
                          ring-coercion/coerce-exceptions-middleware
                          ring-coercion/coerce-response-middleware
                          ring-coercion/coerce-request-middleware
                          [wrap-request-filter request-filter]]}})))

(comment
  (user/restart!)
  (user/restart! :system/router)

  (:result
   (reitit.core/match-by-path
    (user/component :system/router)
    "/.well-known/jwks.json"))
  )
