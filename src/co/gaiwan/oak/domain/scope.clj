(ns co.gaiwan.oak.domain.scope
  (:require
   [clojure.string :as str]))

(def openid-scopes
  {"openid" "Sign in to your account"
   "email" "See your email address"
   "profile" "See your personal information"
   "offline_access" "Set long-term access to your account"})

(defn desc [scope]
  (get openid-scopes scope scope))

(defn scope-set [scopes]
  (cond
    (set? scopes)
    scopes

    (string? scopes)
    (set (str/split (str/trim scopes) #"\s+"))

    (coll? scopes)
    (set scopes)))

(defn subset?
  "Is first arg subset of second arg, takes scopes either as collections or
  as (space-separated) strings"
  [requested-scopes granted-scopes]
  (empty? (remove (scope-set granted-scopes)
                  (scope-set requested-scopes))))

(comment
  (subset? "email openid" "email profile openid")
  ;; => true
  (subset? "email profile openid" "email openid")
  ;; => false
  (subset? "email foo openid" "email profile openid")
  ;; => false
  (subset? "email foo openid" "email profile openid")

  )
