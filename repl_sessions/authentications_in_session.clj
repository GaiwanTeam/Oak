(ns repl-sessions.authentications-in-session)

;; currently in session
{:identity #uuid "...."
 :auth-time #inst "..."}

;; in request
{:identity {... identity entity ...}}

;; logged in = binary

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; new plan!

;; logged in = gradient

;; in session
{;; - explicitly track auth/credential challenges
 ;; - allow being logged in to multiple accounts
 :authentications
 {#uuid "identity-id" #{{:type "password"
                         :time #inst "..."}
                        {:type "totp"
                         :device ...
                         :trust-device? ...
                         :time #inst "..."}
                        {:type "backup_code"
                         :time #inst "..."}
                        {:type "magic_link"
                         :time #inst "..."}
                        {:type "otp"
                         :time #inst "..."}}

  #uuid "other-identity" #{{:type "password" ,,,}}
  }

 ;; will need a way to still track which is the currently active identity
 ;; but this by itself no longer signals that you are "logged in"
 :identity #uuid "..."
 }

;; request
{:authentications
 {"password" {:created-at ...}
  "totp" {:created-at ...}}}
