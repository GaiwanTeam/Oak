(ns co.gaiwan.oak.system.security-providers
  "Allow adding Java security providers through configuration

  E.g. you might want to set BouncyCastle as the default, for FIPS Compliance.

  Config keys: `:java.security.provider/prepend` /
  `:java.security.provider/append`. Both take a collection of strings, which are
  either a class name (assuming a zero-arg constructor), or
  className/methodName, assuming a zero-arg static method for
  initialization (e.g. Conscrypt)
  "
  (:require
   [clojure.string :as str]
   [co.gaiwan.oak.util.log :as log])
  (:import
   (java.lang.reflect Constructor)
   (java.security Provider Security)))

(set! *warn-on-reflection* true)

(defn instantiate-provider [class-or-method-name]
  (if (str/includes? class-or-method-name "/")
    (let [[klz meth] (str/split class-or-method-name #"/")]
      (if-let [klz (Class/forName klz)]
        (if-let [meth (.getMethod klz meth (into-array Class []))]
          (.invoke meth nil (into-array Object []))
          (throw (IllegalArgumentException. (str "Failed to load SecurityProvider " klz ", no zero-arg method " meth))))
        (throw (IllegalArgumentException. (str "Failed to load SecurityProvider " klz)))))

    (if-let [klz (Class/forName class-or-method-name)]
      (.newInstance ^Constructor (first (.getConstructors klz)) (into-array Object []))
      (throw (IllegalArgumentException. (str "Failed to load SecurityProvider " class-or-method-name))))))

(def component
  {:start
   (fn [{:keys [prepend append]}]
     (doseq [p (reverse prepend)]
       (Security/insertProviderAt (instantiate-provider p) 1))
     (doseq [a append]
       (Security/addProvider (instantiate-provider a)))
     (doseq [[idx ^Provider provider] (map-indexed vector (Security/getProviders))]
       (log/info :java.security/provider {:idx idx :name (.getName provider) :version (.getVersion provider)})))})
