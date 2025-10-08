# Oak IAM

Rooted in Standards, Built to Last 


## Dev

Start the application

```
echo '{:launchpad/aliases [:dev :test]}' > deps.local.edn
bin/launchpad
```

Initial setup

```
bin/oakadm jwk create
bin/oakadm oauth-client create 
bin/oakadm oauth-client create --client-name "foo" --redirect-uri 'https://example.com/redirect' --scope email --scope openid --scope offline_access
bin/oakadm user create --email foo@bar.com --password abc
```

## Running in prod mode

When running from source, Oak assumes it runs in a dev environment, when
deploying in production you want to make sure it uses the `prod` environment
instead, which will affect configuration defaults. You can do this by setting
the `OAK__ENV=prod` environment variable, or the `oak.env=prod` java system
property. (`-J-Doak.env=prod`).

Future docker containers instead will default to `prod`, so they use sensible
defaults for a production scenario.

## Configuring the Database Connection

To connect to your database, you need to provide a configuration map under the
`:db/url`, `:db/username`, and `:db/password` keys.

A basic configuration looks like this (this is the default configuration in dev,
which matches the docker-compose setup):

```clj
{:db/url "jdbc:postgresql://localhost:5432/oak"
 :db/username "oak"
 :db/password "oak"}
```

## Configuring Security Providers

You can dynamically register Java Cryptography Architecture (JCA) providers at
runtime. This is useful for ensuring that your application uses a specific
security provider, such as BouncyCastle for FIPS compliance, without modifying
the JVM's `java.security` file.

Configuration is handled through two keys: `:java.security.provider/prepend` and
`:java.security.provider/append`. The order of providers is significant;
providers specified with `:prepend` will be given priority over existing and
appended providers.

Both keys accept a collection of strings, where each string specifies a provider
to be initialized. There are two supported formats for the provider string:

1.  **`className`**: For providers that can be initialized with a zero-argument constructor.

    * Example: `"org.bouncycastle.jce.provider.BouncyCastleProvider"`

2.  **`className/methodName`**: For providers that are initialized by calling a static, zero-argument method.

    * Example: `"org.conscrypt.Conscrypt/newProvider"`

### Example Configuration

The following example demonstrates how to prepend the BouncyCastle provider (giving it the highest priority) and append the Conscrypt provider.

```clojure
{:java.security.provider/prepend ["org.bouncycastle.jce.provider.BouncyCastleProvider"]
 :java.security.provider/append  ["org.conscrypt.Conscrypt/newProvider"]}
```

# API docs

```
npx @redocly/cli build-docs 'http://127.0.0.1:4800/openapi.json'
```

# License

Copyright &copy; 2025 Arne Brasseur

All code in this project is made available under the Apache 2.0 license, unless
specified otherwise. See LICENSE.txt.
