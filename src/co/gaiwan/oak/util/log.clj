(ns co.gaiwan.oak.util.log
  "Provides a structured, key-value logging interface on top of Log4j2.
  Inspired by pedestal.log and lambdaisland.glogi.

  Usage:
    (require '[co.gaiwan.oak.util.log :as log])

    (log/info :app/starting {:port 8080 :env :prod})
    (try
      (/ 1 0)
      (catch Exception e
        (log/error :db/query-failed :query \"SELECT * FROM users\" :exception e)))

  Built to work with structured output, in particular JsonLayout "
  (:import
   (org.apache.logging.log4j Level LogManager)
   (org.apache.logging.log4j.message MapMessage)))

(defn- deep-stringify-keys
  "Recursively converts map keys to strings for Log4j2's MapMessage,
  preserving nested map values."
  [m]
  (let [f (fn [[k v]]
            (let [new-v (if (map? v) (deep-stringify-keys v) v)]
              [(if (keyword? k) (str (.-sym k)) (str k)) new-v]))]
    (into {} (map f m))))

(defonce ^:private loggers (atom {}))

(defn- get-logger
  "Gets a Log4j2 logger for the given namespace string. Caches logger instances."
  [^String ns-str]
  (or (@loggers ns-str)
      (let [logger (LogManager/getLogger ns-str)]
        (swap! loggers assoc ns-str logger)
        logger)))

(defn log*
  "Internal logging function. Do not use directly.
  Handles level check, data conversion, and the actual log call."
  [level ns-str line & kvs]
  (let [logger        (get-logger ns-str)
        log-level-obj (Level/toLevel (name level) Level/OFF)]
    ;; Performance: only construct the message if the level is enabled.
    (when (.isEnabled logger log-level-obj)
      (let [data      (apply hash-map kvs)
            exception (get data :exception)
            log-data  (dissoc data :exception)
            log-data  (if-let [ed (and exception (ex-data exception))]
                        (assoc log-data :ex-data ed)
                        log-data)
            final-map (deep-stringify-keys
                       (assoc log-data :line line))
            map-msg   (MapMessage. final-map)]
        (if (and exception (instance? Throwable exception))
          (.log logger log-level-obj map-msg exception)
          (.log logger log-level-obj map-msg))))))

(defmacro trace
  "Logs a trace message with key-value pairs."
  [& kvs]
  `(log* :trace ~(name (ns-name *ns*)) ~(:line (meta &form)) ~@kvs))

(defmacro debug
  "Logs a debug message with key-value pairs."
  [& kvs]
  `(log* :debug ~(name (ns-name *ns*)) ~(:line (meta &form)) ~@kvs))

(defmacro info
  "Logs an info message with key-value pairs."
  [& kvs]
  `(log* :info ~(name (ns-name *ns*)) ~(:line (meta &form)) ~@kvs))

(defmacro warn
  "Logs a warn message with key-value pairs."
  [& kvs]
  `(log* :warn ~(name (ns-name *ns*)) ~(:line (meta &form)) ~@kvs))

(defmacro error
  "Logs an error message with key-value pairs.
  Use the `:exception` key to pass a Throwable for stack trace logging."
  [& kvs]
  `(log* :error ~(name (ns-name *ns*)) ~(:line (meta &form)) ~@kvs))

(defmacro fatal
  "Logs a fatal message with key-value pairs.
  Use the `:exception` key to pass a Throwable for stack trace logging."
  [& kvs]
  `(log* :fatal ~(name (ns-name *ns*)) ~(:line (meta &form)) ~@kvs))
