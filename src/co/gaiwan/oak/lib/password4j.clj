(ns co.gaiwan.oak.lib.password4j
  "Wrapper around password4j that can detect the hash type based on the Modular
  Crypt Format (MCF) prefix.

  Pepper and other settings can be configured through Password4j's own mechanisms,
  `-J-Dpsw4j.configuration=/my/path/to/password4j.properties`
  "
  (:import
   (com.password4j Password HashBuilder)))

(defn hash-password [password hash-type]
  (let [builder (Password/hash ^String password)]
    (.getResult
     ^Hash
     (case hash-type
       :argon2
       (.. builder
           addRandomSalt
           addPepper
           withArgon2)
       :bcrypt
       (.withBcrypt builder)
       :scrypt
       (.. builder
           addRandomSalt
           addPepper
           withScrypt)))))

(defn check-password [^String password ^String hash]
  (let [check (Password/check password hash)]
    (case (subs hash 0 2)
      "$a"
      (.withArgon2 check)
      "$2"
      (.withBcrypt check)
      "$1"
      (.withScrypt check))))
