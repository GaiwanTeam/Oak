# Oak IAM

Rooted in Standards, Built to Last 


## Dev

PostgreSQL container:

```
docker run -i -p 5432:5432 --name=oak-postgres \
  -v ./pg-data:/var/lib/postgresql/data 
  -v ./dev/init_postgres.sql:/docker-entrypoint-initdb.d/init.sql \
  postgres:17
```

## Configuring the Database Connection

To connect to your database, you need to provide a configuration map under the `:db/config` key. At a minimum, this includes the JDBC URL, username, and password.

A basic configuration looks like this:

```clj
{:db/config {:url "jdbc:postgresql://localhost:5432/oak"
 :username "oak"
 :password "oak"}}
```

### Advanced Configuration

For more fine-grained control over the connection pool, you can specify
additional options. We use HikariCP for connection pooling, and you can pass
most of its configuration settings directly. These are provided as keywords
within the `:db/config` map.

The available options are grouped by function below.

**Core Connection Settings**

| Keyword | Description | Type |
| :--- | :--- | :--- |
| `:catalog` | Sets the default catalog for the connection. | `string` |
| `:connection-init-sql` | A SQL statement that is executed after every new connection is created. | `string` |
| `:read-only` | Sets the connection to read-only mode. | `boolean` |
| `:schema` | Sets the default schema for the connection. | `string` |

**Pool Sizing & Behavior**

| Keyword | Description | Type |
| :--- | :--- | :--- |
| `:maximum-pool-size` | The maximum size that the pool is allowed to reach, including both idle and in-use connections. | `int` |
| `:minimum-idle` | The minimum number of idle connections that HikariCP tries to maintain in the pool. | `int` |
| `:pool-name` | A name for the connection pool. | `string` |

**Timeout Settings**

| Keyword | Description | Type |
| :--- | :--- | :--- |
| `:connection-timeout` | The maximum number of milliseconds that a client will wait for a connection from the pool. | `long` |
| `:idle-timeout` | The maximum amount of time (in milliseconds) that a connection will be tested for validity. | `long` |

### Example with Advanced Options

Here is a more complex configuration example for a production environment, tuning the pool size and timeouts.

```
{:db/config {:url "jdbc:postgresql://db.prod:5432/myapp"
 :username "prod_user"
 :password "secret_password"
 :maximum-pool-size 20
 :minimum-idle 5
 :idle-timeout 600000
 :connection-timeout 30000
 :pool-name "my-app-pool"}}
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

# License

Copyright &copy; 2025 Arne Brasseur

All code in this project is made available under the Apache 2.0 license, unless
specified otherwise. See LICENSE.txt.
