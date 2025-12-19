# Configuring Oak

Oak can be configured through environment variables or configuration files.
There shouldn't be a whole lot you need to configure yourself, Oak comes with
sensible defaults built-in.

Oak's configuration files are in the [EDN](https://github.com/edn-format/edn)
file format. EDN is comparable to JSON, with some differences:

- Keys are written with an initial colon, no quotation marks, and no final colon:
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
 :http/host "0.0.0.0"}
```

The following locations are searched for a configuration file:

- `$XDG_CONFIG_HOME/oak.edn` (defaults to: `$HOME/.config/oak.edn`)
- `/etc/oak.edn`

When running the container, an easy way to configure things is by mounting
`/etc/oak.edn` into the container.
