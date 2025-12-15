(ns co.gaiwan.oak.apis.inspect)

(defn GET-inspect-session [req]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (with-out-str (clojure.pprint/pprint (:session req)))})

(defn component [opts]
  (when (:enabled opts)
    {:routes
     ["/inspect" {}
      ["/session"
       {:name :inspect/session
        :auth-claim :public
        :get #'GET-inspect-session}]]}))
