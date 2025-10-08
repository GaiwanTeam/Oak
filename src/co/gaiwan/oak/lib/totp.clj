(ns co.gaiwan.oak.lib.totp
  "A library generate, verify time-based one time passwords for
   Multi-Factor Authentication."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (dev.samstevens.totp.code DefaultCodeGenerator DefaultCodeVerifier HashingAlgorithm)
   (dev.samstevens.totp.qr QrData$Builder ZxingPngQrGenerator)
   (dev.samstevens.totp.secret DefaultSecretGenerator)
   (dev.samstevens.totp.time SystemTimeProvider)
   (dev.samstevens.totp.util Utils)))

(set! *warn-on-reflection* true)

(defn secret
  "Generate a shared secret.
   A shared secret is to be given to the user to add to an MFA application"
  [hash-alg size]
  (str hash-alg ":" (.generate (DefaultSecretGenerator. size))))

(defn hash-alg [k]
  (case k
    "SHA-1"
    HashingAlgorithm/SHA1
    "SHA-256"
    HashingAlgorithm/SHA256
    "SHA-512"
    HashingAlgorithm/SHA512))

(defn qrdata [{:keys [label secret issuer digits period]
               :or {digits 6 period 30}}]
  (let [[alg key] (str/split secret #":")
        builder (QrData$Builder.)]
    (-> builder
        (.label label)
        (.secret key)
        (.issuer issuer)
        (.algorithm (hash-alg alg))
        (.digits digits)
        (.period period)
        (.build))))

(defn qrcode-as-bytes
  "Returns the QR image which used to transfer the shared secret"
  [{:keys [label secret] :as opts}]
  (let [qrdata (qrdata opts)]
    (.generate (ZxingPngQrGenerator.) qrdata)))

(defn qrcode-data-url [opts]
  (let [qrdata (qrdata opts)
        generator (ZxingPngQrGenerator.)]
    (Utils/getDataUriForImage
     (.generate generator qrdata)
     (.getImageMimeType generator))))

(comment
  (with-open [png (io/output-stream (io/file "/tmp/qrcode.png"))]
    (.write png (qrcode-as-bytes {}))))

(defn verify-code
  "Verify TOTP code supplied by the user from their Authenticator app.
  - `secret` is a self-contained hash-alg+secret string
  - `code` code supplied by user, typically 6 digit string
  "
  [secret code]
  (let [[alg secret] (str/split secret #":")]
    (.isValidCode
     (DefaultCodeVerifier.
      (DefaultCodeGenerator. (hash-alg alg))
      (SystemTimeProvider.))
     secret
     code)))
