# Setup JWK / Users / Applications

Before you can start using Oak, you need to set up a few things: a JWK
public/private key to sign auth tokens, user accounts, and OAuth applications
you want to access through Oak.

This is all done through the `oakadm` tool. Assuming you have Oak running as a
podman container named `oak`, you can do the following. (Replace "podman" with
"docker" if that's what you are using).

```
podman exec -it oak oakadm --help
```

## JWK

JSON Web Keys (JWKs) are used to cryptographically sign JWT tokens issued by Oak. You'll need to create at least one JWK to get started.

Create a default JWK with:

```bash
podman exec -it oak oakadm jwk create
```

This creates an EdDSA key using Ed25519 (OKP key type with EdDSA algorithm), which is the recommended default.

For more control, you can specify the key type and parameters:

- **RSA keys**: `--kty RSA --size 2048 --alg RS256`
- **ECDSA keys**: `--kty EC --crv P-256 --alg ES256`
- **EdDSA keys**: `--kty OKP --crv Ed25519 --alg EdDSA`

Example creating an RSA key:

```bash
podman exec -it oak oakadm jwk create --kty RSA --size 2048 --alg RS256 --default
```

RSA is generally the most widely recognized key type, if you have other services
that need to be able to validate Oak's tokens and if they don't yet understand
the OKP key, then you can use RSA instead.

The `--default` flag makes this key the primary one used for signing new tokens. List all JWKs with:

```bash
podman exec -it oak oakadm jwk list
```

## Users

Create user accounts that can authenticate with Oak. Each user needs at least an
email address. You can set a password here, or leave it unset and let the user
request a password reset email themselves (recommended). 

```bash
# Create a basic user
podman exec -it oak oakadm user create --email user@example.com --password securepassword123

# Create user with additional claims
podman exec -it oak oakadm user create \
  --email admin@example.com \
  --password admin123 \
  --claim role=admin \
  --claim department=engineering
```

Required parameter:

- `--email`: The user's email address (primary identifier)

Optional parameters:

- `--password`: Set the user's password directly 
- `--claim`: Add custom OAuth claims as key=value pairs (can be used multiple times)

List all users:

```bash
podman exec -it oak oakadm user list
```

## OAuth Application

Create OAuth 2.0 / OpenID Connect clients for your applications that need to authenticate users through Oak:

```bash
# Basic client with minimal configuration
podman exec -it oak oakadm oauth-client create --client-name "My Web App"

# Full-featured client with all common settings
podman exec -it oak oauth-client create \
  --client-name "My Application" \
  --redirect-uri "https://myapp.com/auth/callback" \
  --redirect-uri "https://myapp.dev/auth/callback" \
  --scope openid \
  --scope email \
  --scope profile \
  --scope offline_access \
  --grant-type authorization_code \
  --grant-type refresh_token \
  --response-type code \
  --token-endpoint-auth-method client_secret_basic \
  --client-uri "https://myapp.com" \
  --logo-uri "https://myapp.com/logo.png"
```

**Required:**
- `--client-name`: Human-readable name for the application

**Important configurations:**
- `--redirect-uri`: Where users are sent after login (can be specified multiple times for different environments)
- `--scope`: Permissions requested (openid, email, profile, offline_access are common)
- `--grant-type`: `authorization_code` for web apps, or `client_credentials` for service-to-service

**Optional:**
- `--token-endpoint-auth-method`: How the client authenticates (`client_secret_basic`, `client_secret_post`, or `none` for public clients like SPAs)
- `--client-uri`: Your application's homepage
- `--logo-uri`: Logo displayed on the login/consent screen

The command outputs the client credentials including the client ID and secret.
Store these securely - you'll need them to integrate your application with Oak.

List all OAuth clients:
```bash
podman exec -it oak oakadm oauth-client list
```

----

< Previous [Configuration](./configuring.md) >
