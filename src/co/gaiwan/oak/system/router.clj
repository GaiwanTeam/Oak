(ns co.gaiwan.oak.system.router
  "HTTP router and middleware setup"
  (:require
   [clojure.string :as str]
   [co.gaiwan.oak.app.config :as config]
   [co.gaiwan.oak.lib.auth-middleware :as auth-mw]
   [co.gaiwan.oak.lib.ring-csp :as ring-csp]
   [co.gaiwan.oak.util.log :as log]
   [muuntaja.core :as muuntaja]
   [muuntaja.format.charred :as muuntaja-charred]
   [reitit.coercion.malli]
   [reitit.openapi :as reitit-openapi]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as ring-coercion]
   [reitit.ring.middleware.muuntaja :as reitit-muuntaja]
   [reitit.ring.middleware.parameters :as reitit-params]
   [ring.middleware.session :as ring-session]
   [ring.util.response :as res]))

(def malli-coercion-options
  {:error-keys #{:type :coercion :in :schema :value :errors :humanized :transformed}})

(defn muuntaja-instance
  "Muuntaja instance used for request and response coercion"
  []
  (muuntaja/create
   (-> muuntaja/default-options
       (assoc-in [:formats "application/json"] muuntaja-charred/format)
       (assoc-in [:formats "application/json; charset=utf-8"] muuntaja-charred/format))))

(defn wrap-request-filter
  "Helper middleware to apply a function to requests, before they go to the
  handler. Used to inject additional things into the request map, like `:db`"
  [handler rf]
  (fn [req]
    (handler (rf req))))

(defn- update-handler [{:keys [handler] :as verb-data}]
  (if (var? handler)
    (merge
     (cond-> verb-data
       (not (config/get :dev/route-var-handlers))
       (update :handler deref))
     (meta handler))
    verb-data))

(defn- compile-var-meta
  "Take route-data, find handlers defined as vars, and expand them with metadata"
  [route-data]
  (reduce (fn [d m]
            (if (contains? d m)
              (do
                (update d m update-handler))
              d))
          route-data
          [:get :post :put :delete]))

(defn reitit-compile-fn
  "Wrap reitit's default compile-fn so we can hook into this to transform routes"
  [[path data] opts]
  (ring/compile-result [path (compile-var-meta data)] opts))

(defn wrap-log-request [h]
  (fn [req]
    (let [start (System/currentTimeMillis)
          res (h req)
          delta (- (System/currentTimeMillis) start)
          type (res/get-header res "content-type")
          location (res/get-header res "location")]
      (log/debug :http/request (cond-> {(keyword (str/upper-case (name (:request-method req))))
                                        (:uri req)
                                        :status (:status res)
                                        :t delta}
                                 type (assoc :type type)
                                 location (assoc :location location)))
      res)))

(defn wrap-404 [h]
  (fn [req]
    (let [res (h req)]
      (if (nil? res)
        {:status 404
         :headers {"Content-Type" "text/plain"}
         :body "404 Not Found"}
        res))))

(defn component [{:keys [routes request-filters session-store]}]
  (let [request-filter (apply comp (keep :http/request-filter request-filters))
        routes         (into ["" {}
                              ["/ping" {:get (constantly {:status 200 :body "pong"})}]]
                             (map :routes routes))]
    (log/info :routes routes)
    (ring/router
     routes
     {:compile reitit-compile-fn
      :data
      {:coercion   (reitit.coercion.malli/create malli-coercion-options)
       :muuntaja   (muuntaja-instance)
       :middleware [wrap-log-request
                    reitit-openapi/openapi-feature
                    reitit-params/parameters-middleware
                    reitit-muuntaja/format-negotiate-middleware
                    reitit-muuntaja/format-response-middleware
                    reitit-muuntaja/format-request-middleware
                    ring-coercion/coerce-exceptions-middleware
                    ring-coercion/coerce-response-middleware
                    ring-coercion/coerce-request-middleware
                    [wrap-request-filter request-filter]
                    [ring-session/wrap-session
                     {:store session-store
                      :cookie-name (config/get :http-session/cookie-name)
                      :cookie-attrs
                      (cond-> {:http-only true
                               :same-site :strict}
                        (config/get :http-session/secure-cookie)
                        (assoc :secure true))}]
                    ring-csp/wrap-content-security-policy
                    wrap-404]}})))

(comment
  (user/restart!)
  (user/restart! :system/router)
  )
