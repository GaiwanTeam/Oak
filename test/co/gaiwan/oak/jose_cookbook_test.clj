(ns co.gaiwan.oak.jose-cookbook-test
  (:require
   [charred.api :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.gitlibs :as git]
   [co.gaiwan.oak.util.base64 :as base64]
   [co.gaiwan.oak.util.jwk :as jwk])
  (:import
   (java.security Security Signature)
   (javax.crypto SecretKey)))

(defn cookbook-dir []
  (git/procure "https://github.com/ietf-jose/cookbook.git"
               'ietf-jose/cookbook
               "13692b68bfc18b99557a5b1ed311fd5077bfff04"))


(def test-cases
  (map #(json/read-json
         %
         :key-fn keyword)
       (filter #(str/ends-with? (str %) ".json")
               (file-seq (io/file (cookbook-dir) "jws")))))

(for [test-case test-cases]
  (try
    (let [{:keys [input signing output]} test-case
          {:keys [key alg payload]} input]
      (crypt/verify
       (crypt/signer (jwk/alg->java alg))
       (jwk/jwk->key key)
       (.getBytes
        (str (base64/encode-str-no-pad
              (json/write-json-str (:protected signing)))
             "."
             (base64/encode-str-no-pad
              payload)))
       (base64/url-decode (:sig signing)))

      )
    (catch Exception e
      (str e)      )))
