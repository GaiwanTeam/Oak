(ns repl-sessions.totp
  (:require
   [clojure.java.io :as io])
  (:import
   (dev.samstevens.totp.code DefaultCodeGenerator DefaultCodeVerifier HashingAlgorithm)
   (dev.samstevens.totp.qr QrData$Builder ZxingPngQrGenerator)
   (dev.samstevens.totp.secret DefaultSecretGenerator)
   (dev.samstevens.totp.time SystemTimeProvider)))

(def secret (.generate (DefaultSecretGenerator. 52)))

(def qrdata
  (let [builder (QrData$Builder.)]
    (-> builder
        (.label "example@example.com")
        (.secret secret)
        (.issuer "AppName")
        (.algorithm HashingAlgorithm/SHA512)
        (.digits 6)
        (.period 30)
        (.build))))

(with-open [png (io/output-stream (io/file "/tmp/qrcode.png"))]
  (.write png (.generate (ZxingPngQrGenerator.) qrdata)))

(.isValidCode
 (DefaultCodeVerifier.
  (DefaultCodeGenerator. HashingAlgorithm/SHA512)
  (SystemTimeProvider.))
 secret
 "619283")
