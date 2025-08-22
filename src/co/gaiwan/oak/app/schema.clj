(ns co.gaiwan.oak.app.schema
  (:require
   [co.gaiwan.oak.domain.jwk :as jwk]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]))

(def schema
  {:jwk jwk/attributes
   :oauth_client oauth-client/attributes})
