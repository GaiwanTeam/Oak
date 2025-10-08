(ns co.gaiwan.oak.lib.totp
  "A library generate, verify time-based one time passwords for
   Multi-Factor Authentication."
  (:require
   [clojure.java.io :as io])
  (:import
   (dev.samstevens.totp.code DefaultCodeGenerator DefaultCodeVerifier HashingAlgorithm)
   (dev.samstevens.totp.qr QrData$Builder ZxingPngQrGenerator)
   (dev.samstevens.totp.secret DefaultSecretGenerator)
   (dev.samstevens.totp.time SystemTimeProvider)))

(set! *warn-on-reflection* true)
(def ^:dynamic *issuer* "example app")

(defn secret
  "Generate a shared secret.
   A shared secret is to be given to the user to add to an MFA application"
  [size]
  (.generate (DefaultSecretGenerator. size)))

(defn qrdata [{:keys [label secret issuer digits period]
               :or {digits 6 period 30}}]
  (let [builder (QrData$Builder.)]
    (-> builder
        (.label label)
        (.secret secret)
        (.issuer issuer)
        (.algorithm HashingAlgorithm/SHA512)
        (.digits digits)
        (.period period)
        (.build))))

(defn qrcode-as-bytes
  "Returns the QR image which used to transfer the shared secret"
  [{:keys [label secret] :as input}]
  (let [qrdata (qrdata (assoc input :issuer *issuer*))]
    (.generate (ZxingPngQrGenerator.) qrdata)))

(comment
  (with-open [png (io/output-stream (io/file "/tmp/qrcode.png"))]
    (.write png (.generate (ZxingPngQrGenerator.) qrdata))))

(defn verify-password
  "verify the code from MFA application comply with the secret"
  [secret code]
  (.isValidCode
   (DefaultCodeVerifier.
    (DefaultCodeGenerator. HashingAlgorithm/SHA512)
    (SystemTimeProvider.))
   secret
   code))


