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

