## Advanced Configuration

These are flags that are not documented in [Configuring Oak](./configuring.md),
and which you generally want to leave to their defaults, unless you know what
you are doing.

```clj
{
 :oauth/access-token-exp-time-seconds 1800
 :openid/id-token-exp-time-seconds    300
 :totp/secret-size                    64
 :password/hash-type                  "argon2" ;; "bcrypt", "scrypt"
 :http-session/cookie-name            "oak-session"
 :http/csp-policy                     "default-src 'self'; script-src 'self' 'nonce-random_value'; img-src 'self' data:;"
 :html/inline-css                     false
 :totp/hash-alg                       "SHA-512"}
```
