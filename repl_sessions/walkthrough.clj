(ns walkthrough
  (:require
   [co.gaiwan.oak.domain.jwk :as jwk]))

(jwk/create! (user/db) {"kty" "RS256"})


{"crv" "Ed25519",
 "x" "2mM-WetAXihSV103V3NnGOIevFb4wP1B3lVygFiM1mA",
 "kty" "OKP",
 "kid" "317erBZtc7i1EY3DltdA6"},

{"crv" "Ed25519",
 "d" "18FgUd3Paa_NombZmV9NlHCQbPCSJhX1S438lr3mroE",
 "x" "2mM-WetAXihSV103V3NnGOIevFb4wP1B3lVygFiM1mA",
 "kty" "OKP",
 "kid" "317erBZtc7i1EY3DltdA6"},
