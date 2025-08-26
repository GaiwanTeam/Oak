(ns co.gaiwan.oak.lib.pg-jsonb
  "PostgreSQL JSONB type handling for next.jdbc"
  (:require
   [charred.api :as json]
   [next.jdbc.prepare :as prepare]
   [next.jdbc.result-set :as result-set])
  (:import
   (clojure.lang Keyword)
   (java.sql PreparedStatement Timestamp)
   (java.time Instant)
   (org.postgresql.util PGobject)))

(defn- clj->pg
  "Convert Clojure collection to PGobject"
  [m]
  (doto (org.postgresql.util.PGobject.)
    (.setType (or (-> m meta :pg/type) "jsonb"))
    (.setValue (json/write-json-str m))))

(defn pg->clj
  "Convert PGObject to Clojure value"
  [^org.postgresql.util.PGobject o]
  (let [type (.getType o)]
    (when o
      (if (#{"jsonb" "json"} type)
        (with-meta (json/read-json (.getValue o)) {:pg/type type})
        (.getValue o)))))

(def settable-param-impl
  {:set-parameter
   (fn [o ^PreparedStatement stmt idx]
     (.setObject stmt idx (clj->pg o)))})

(extend clojure.lang.IPersistentMap prepare/SettableParameter settable-param-impl)
(extend clojure.lang.IPersistentVector prepare/SettableParameter settable-param-impl)
(extend clojure.lang.IPersistentList prepare/SettableParameter settable-param-impl)
(extend clojure.lang.IPersistentSet prepare/SettableParameter settable-param-impl)
(extend clojure.lang.LazySeq prepare/SettableParameter settable-param-impl)

(extend-protocol prepare/SettableParameter
  Keyword
  (set-parameter [^Keyword v ^PreparedStatement ps idx]
    (.setString ps idx (.toString (.-sym v))))

  Instant
  (set-parameter [^Instant v ^PreparedStatement ps idx]
    (.setTimestamp ps idx (Timestamp/from v))))

(extend-type org.postgresql.util.PGobject
  result-set/ReadableColumn
  (read-column-by-label [^org.postgresql.util.PGobject o _]
    (pg->clj o))
  (read-column-by-index [^org.postgresql.util.PGobject o _ _]
    (pg->clj o)))