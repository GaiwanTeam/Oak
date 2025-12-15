(ns co.gaiwan.oak.apis.openapi
  "Standard API description"
  (:require
   [clojure.walk :as walk]
   [reitit.openapi :as openapi]))

(defn component [opts]
  (let [handler (openapi/create-openapi-handler)]
    {:routes ["/openapi.json"
              {:get
                {:no-doc true
                 :auth-claim :public
                 :handler (fn [req]
                            (let [res (handler req)]
                              ;; Charred doesn't like numbers as map keys (e.g.
                              ;; {:responses {200 ...}}), so stringify them
                              ;; beforehand
                              (walk/postwalk
                               (fn [o]
                                 (if (map? o)
                                   (update-keys o (fn [k]
                                                    (if (keyword? k) k (str k))))
                                   o))
                               res)))
                 :openapi {:info {:title "Oak API"
                                  :version "1.0.0"}}}}]}))
