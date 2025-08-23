(ns co.gaiwan.oak.app.schema
  (:require
   [co.gaiwan.oak.domain.credential :as credential]
   [co.gaiwan.oak.domain.identity :as identity]
   [co.gaiwan.oak.domain.identifier :as identifier]
   [co.gaiwan.oak.domain.jwk :as jwk]
   [co.gaiwan.oak.domain.oauth-client :as oauth-client]))

(def schema
  [[:jwk jwk/attributes]
   [:oauth_client oauth-client/attributes]
   [:identity identity/attributes]
   [:identifier identifier/attributes]
   [:credential credential/attributes]
   ])

(comment
  (user/restart! :system/database))
