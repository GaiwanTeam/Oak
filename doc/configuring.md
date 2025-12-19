# Configuring Oak

Oak can be configured through environment variables or configuration files.
There shouldn't be a whole lot you need to configure yourself, Oak comes with
sensible defaults built-in.

Oak's configuration files are in the [EDN](https://github.com/edn-format/edn)
file format. EDN is comparable to JSON, with some differences:

- Keys are written with an initial colon, no quotation marks, and no final colon
- Commas are optional and can be omitted
- Comments start with one or more semicolons: `; this is a comment`

**JSON**:

```json
{"http/port": 4800,
 "http/host": "0.0.0.0"}
```

**EDN**

```edn
{:http/port 4800
 :http/host "0.0.0.0" ; listen on all interfaces
 }
```

## Configuration file location

The following locations are searched for a configuration file:

- `$XDG_CONFIG_HOME/oak.edn` (defaults to: `$HOME/.config/oak.edn`)
- `/etc/oak.edn`

When running the container, an easy way to configure things is by mounting
`/etc/oak.edn` into the container.

Podman quadlet:

```ini
[Container]
...
Volume=%h/path/to/oak.edn:/etc/oak.edn:Z
```

docker-compose.yml

```yml
services:
  oak:
    volumes:
      - /path/to/oak.edn:/etc/oak.edn:ro
```

## Environment variables

In the configuration files, configuration keys have a prefix and suffix,
separated by a slash. These map to environment variables as
`OAK__PREFIX__SUFFIX`. Dashes are converted to underscores. For example:

* `:http/port` -> `OAK__HTTP__PORT`
* `:db/url` -> `OAK__DB__URL`
* `:email/smtp-host` -> `OAK__EMAIL__SMTP_HOST`

Environment variables take precedence over configuration files, so if you have
an option configured in both `oak.edn` and specified via an env var, the env var
"wins".

## Example `oak.edn`

Here is a `oak.edn` file you can copy-paste and tweak from there. All values
shown are the built-in defaults, and so could be omitted if they're not changed
from their default value.

```clj
{:db/url      "jdbc:postgresql://localhost:5432/oak"
 :db/username "oak"
 :db/password "oak"
 
 :application/name "Oak"
 :org/name         "Oak"
 :time/zone-id     "UTC"
 
 :http/host "0.0.0.0"
 :http/port 4800
 
 ;; Not set by default, but inferred from request
 ;; :http/origin "http://localhost:4800"
 
 ;; Not set by default, must be explicitly set
 ;; :email/smtp-host "127.0.0.1"
 ;; :email/smtp-port 2525
 ;; :email/from "oak@example.com"
 
 :redis/host       "localhost"
 :redis/port       6379
 :redis/timeout-ms 1000
 
 :password-reset/link-expiry-hours 24
 :password/min-length              6}
```

## Configuration Options

### `:db/url` / `OAK__DB__URL`

JDBC URL to connect to the database. Oak currently only supports PostgreSQL.
Typically this is in the format `jdbc:postgresql://host:port/dbname`.

### `:db/username` / `OAK__DB__USERNAME` ; `:db/password` / `OAK__DB__PASSWORD`

Username and to connect to PostgreSQL. Can be omitted, e.g. when using PAM authentication.

Default: `"oak"` / `"oak"`

### `:application/name` / `OAK__APPLICATION__NAME`

Application name used in the UI and emails. This appears in page titles, email subjects, and other user-facing text.

Default: `"Oak"`

### `:org/name` / `OAK__ORG__NAME`

Organization name used in the UI and emails. This appears in email footers and in OAuth consent flows.

Default: `"Oak"`

### `:time/zone-id` / `OAK__TIME__ZONE_ID`

Determines how timestamps are presented in the UI. Must be a valid [IANA time zone identifier](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones).

Default: `"UTC"`

### `:http/host` / `OAK__HTTP__HOST`

Host the HTTP server listens on. Determines the network interfaces the application listens on. Use "0.0.0.0" to listen on all interfaces.

Default: `"0.0.0.0"`

### `:http/port` / `OAK__HTTP__PORT`

Port the HTTP server listens on.

Default: `4800`

### `:http/origin` / `OAK__HTTP__ORIGIN`

Used for generating OAuth 2.0/OIDC discovery metadata. Can generally be omitted, in which case we determine the hostname from the request. If behind a proxy which doesn't properly propagate the hostname the request is made on, then this may need to be set. Honors the `Host`, `x-forwarded-proto`, and `x-forwarded-host` headers, or the HTTP 2.0 "authority".

Example: `"https://auth.example.com"`

No default value - automatically inferred from request when not set.

### `:email/smtp-host` / `OAK__EMAIL__SMTP_HOST`

Hostname or IP address of the SMTP server used for sending password reset and other transactional emails.

No default value - must be explicitly set to enable email functionality.

### `:email/smtp-port` / `OAK__EMAIL__SMTP_PORT`

Port number of the SMTP server.

No default value - must be explicitly set.

### `:email/from` / `OAK__EMAIL__FROM`

From address used in sent emails. This should be a valid email address that your SMTP server is configured to accept.

No default value - must be explicitly set.

### `:redis/host` / `OAK__REDIS__HOST`

Hostname or IP address of the Redis server used for HTTP session storage.

Default: `"localhost"`

### `:redis/port` / `OAK__REDIS__PORT`

Port number of the Redis server.

Default: `6379`

### `:redis/timeout-ms` / `OAK__REDIS__TIMEOUT_MS`

Redis connection timeout in milliseconds.

Default: `1000`

### `:password-reset/link-expiry-hours` / `OAK__PASSWORD_RESET__LINK_EXPIRY_HOURS`

How long password reset links are valid, in hours. After this time period elapses, password reset tokens become invalid and the user must request a new reset link.

Default: `24`

### `:password/min-length` / `OAK__PASSWORD__MIN_LENGTH`

Minimum required length for passwords. Users must choose passwords that meet this length requirement during registration and password changes.

Default: `6`

----

Next: [Initial Setup](./setup.md)
