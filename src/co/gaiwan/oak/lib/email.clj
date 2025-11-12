(ns co.gaiwan.oak.lib.email
  "Handle (outgoing) email"
  (:require
   [clojure.string :as str]
   [co.gaiwan.oak.app.config :as config]
   [lambdaisland.hiccup :as hiccup]
   [tarayo.core :as smtp]))

(defn smtp-config []
  (let [host (config/get :email/smtp-host)
        user (config/get :email/smtp-user)
        password (config/get :email/smtp-password)
        port (config/get :email/smtp-port)
        type (config/get :email/smtp-type)]
    (assert host)
    (cond-> {:host host}
      user (assoc :user user)
      password (assoc :password password)
      port (assoc :port port)
      (= :ssl type) (assoc :ssl true)
      (= :tls type) (assoc :tls true))))

(defn hiccup->text [h]
  (cond
    (string? h) h
    (vector? h)
    (let [[tag & children] h
          props (if (map? (first children)) (first children) nil)
          children (if props (next children) children)]
      (case tag
        :head ""
        :p (str (apply str (map hiccup->text children)) "\n\n")
        :br "\n"
        :hr "\n\n-----------------------------------------------------\n\n"
        :a
        (let [caption (str (apply str (map hiccup->text children)))
              href (:href props) ]
          (if (= href (str/trim caption))
            caption
            (str caption " (" href ")")))
        (apply str (map hiccup->text children))
        ))
    :else
    (str h)))

(defn send!
  "Send out an email.

  Generally follows the API from [tarayo.core/send!]. `:from` can be omitted,
  it's taken from the `:email/from` config value. You can pass hiccup as
  `:html`, which will automatically extract text to create a html+plain-text
  multipart email."
  [msg]
  ;; TODO: log mails and errors
  (with-open [conn (smtp/connect (smtp-config))]
    (smtp/send!
     conn
     (cond-> msg
       (not (:from msg))
       (assoc :from (config/get :email/from))
       (:html msg)
       (assoc :multipart "alternative"
              :body
              [{:content-type "text/plain" :content (hiccup->text (:html msg))}
               {:content-type "text/html" :content
                (hiccup/render (:html msg))}])))))


(comment
  (send! {:to "arne@gaiwan.co"
          :subject "hello"
          :html [:p "test"]})

  (config/get :email/smtp-host)
  (smtp-config)
  )
