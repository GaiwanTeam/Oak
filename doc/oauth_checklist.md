This is an updated test plan for a server, incorporating best current practices
from **RFC 9700** and the **OAuth 2.1 Authorization Framework**. This updated
checklist removes the deprecated Implicit Grant flow and adds new requirements,
recommendations, and security considerations.

## General
* **REQUIRED:**
    * The server **MUST** use **HTTPS** for all communication.
    * The server **MUST** support the **Authorization Code Grant with PKCE** (`response_type=code`). This is the only grant type recommended for public clients (e.g., single-page applications, native apps).
    * Redirect URIs **MUST** be validated using an **exact string match** against a pre-registered list. Wildcard matching and partial path matching are **NOT RECOMMENDED**.
    * The `state` parameter is now a **REQUIRED** element to protect against CSRF attacks.
    * The server **MUST NOT** support the **Implicit Grant** (`response_type=token`). Verify that it is not supported.

* **RECOMMENDED:**
    * Use a short lifetime for access tokens and a longer lifetime for refresh tokens.
    * Implement **refresh token rotation** to improve security. After a refresh token is used, a new one is issued, and the old one is invalidated.
    * Use **sender-constrained access tokens** to mitigate token theft and replay attacks.
    * Consider using **Pushed Authorization Requests (PAR)** to prevent exposing authorization request parameters in the browser's URL.
    * Avoid issuing bearer tokens in URL query parameters.

* **Error Responses:**
    * All error responses **MUST** follow the format defined in RFC 6749, including the `error` and `error_description` parameters.
    * The server **MUST** return appropriate HTTP status codes (e.g., `400 Bad Request`, `401 Unauthorized`).

---

## Authorization Endpoint
* **REQUIRED:**
    * The server **MUST** reject requests with `response_type=token` as the Implicit Grant is no longer considered secure.
    * Verify that the `client_id` parameter is validated and corresponds to a registered client.
    * The `redirect_uri` **MUST** be validated using **exact string matching**.
    * The `scope` parameter **MUST** be supported and correctly interpreted.
    * The `state` parameter is now **REQUIRED** and must be returned unchanged in the authorization response.
    * The server **MUST** validate the `code_challenge` and `code_challenge_method` parameters from the PKCE flow.
* **Error Responses:**
    * Validate that if a `response_type` other than `code` is used, the server returns an `unsupported_response_type` error.
    * Check that if the `code_challenge` is invalid or missing for a public client, the server returns an `invalid_request` error.
    * Confirm that if the `redirect_uri` does not exactly match a pre-registered URI, the server returns an `invalid_request` error.

---

## Token Endpoint
* **REQUIRED:**
    * Test that the server handles the `grant_type` parameter correctly (`authorization_code` and `refresh_token` are the primary types). The **Resource Owner Password Credentials Grant** and **Implicit Grant** are no longer recommended.
    * The `client_id` and `client_secret` (for confidential clients) **MUST** be correctly authenticated.
    * The server **MUST** return a new `access_token` and `token_type` in a JSON object.
    * The `expires_in` parameter **MUST** be included.
    * For the Authorization Code Grant, the server **MUST** validate the `code_verifier` from the client against the `code_challenge` provided in the authorization request.
    * Using an invalid or expired `authorization_code` **MUST** result in an `invalid_grant` error.
* **RECOMMENDED:**
    * **Refresh token rotation** should be implemented, issuing a new refresh token with each refresh token grant.
* **Error Responses:**
    * Validate that an invalid `grant_type` returns an `unsupported_grant_type` error.
    * Check that if the client authentication fails, the server returns an `invalid_client` error.
    * Confirm that a missing or invalid `redirect_uri` returns an `invalid_request` error.

---

## Access Token
* **REQUIRED:**
    * The access token must be a cryptographically random, non-guessable string.
    * The token must be validated by the resource server before granting access to a protected resource.
    * The access token **MUST** expire after the time specified in `expires_in`.
    * A resource server **MUST** return an `invalid_token` error for an expired or invalid access token.

---

## Refresh Token
* **REQUIRED:**
    * A client **MUST** be able to use a valid refresh token at the token endpoint to obtain a new `access_token`.
    * Using an expired or invalid refresh token **MUST** result in an `invalid_grant` error.
* **RECOMMENDED:**
    * Implement **refresh token rotation**: after a refresh token is used, it should be revoked to prevent its reuse. The server should then issue a new refresh token.
