## JOSE

JSON Object Signing and Encryption (JOSE) is a framework for securely transmitting data between two parties. It's a collection of IETF standards that provide a secure way to represent claims to be transferred between parties. It includes three main specifications: JSON Web Signature (JWS) for signing, JSON Web Encryption (JWE) for encryption, and JSON Web Key (JWK) for representing cryptographic keys. A common use case is creating and verifying JSON Web Tokens (JWTs), which are often used in OAuth 2.0 and OIDC to represent identity or authorization information.

History: JOSE was developed by the IETF JOSE working group to provide a more compact and URL-safe way to secure data compared to XML-based security standards like XML Digital Signature and XML Encryption. The working group was formed in 2010, and the core RFCs were published in 2015.

Standard Documents:

- RFC 7515: JSON Web Signature (JWS)
- RFC 7516: JSON Web Encryption (JWE)
- RFC 7517: JSON Web Key (JWK)
- RFC 7518: JSON Web Algorithms (JWA)
- RFC 7519: JSON Web Token (JWT)

## OAuth 2.0

OAuth 2.0 is an authorization framework that allows a third-party application to get limited access to an HTTP service on behalf of a resource owner. It's not an authentication protocol itself; it's about granting access to resources. For example, when you use "Sign in with Google" on a third-party app, you're using OAuth 2.0 to grant that app permission to access some of your Google data (like your profile) without sharing your Google password. The process involves an authorization server, a resource server, a client, and the resource owner.

History: The first version, OAuth 1.0, was released in 2010. OAuth 2.0 was developed to address complexities and security issues with 1.0, offering a simpler, more flexible, and more scalable framework. The IETF OAuth working group published the core RFC in 2012.

Standard Documents:

- RFC 6749: The OAuth 2.0 Authorization Framework
- RFC 6750: The OAuth 2.0 Authorization Framework: Bearer Token Usage
- RFC 7009: OAuth 2.0 Token Revocation
- RFC 7591: OAuth 2.0 Dynamic Client Registration Protocol
- RFC 7636: Proof Key for Code Exchange (PKCE) by OAuth Public Clients
- RFC 7662: OAuth 2.0 Token Introspection specification, detailed in 
- RFC 8414: OAuth 2.0 Authorization Server Metadata
- RFC 8628: OAuth 2.0 Device Authorization Grant
- RFC 8705: OAuth 2.0 Mutual-TLS Client Authentication and Certificate-Bound Access Tokens (mTLS)
- RFC 8252: OAuth 2.0 for Native Apps
- RFC 9068: JSON Web Token (JWT) Profile for OAuth 2.0 Access Tokens

## OIDC

OpenID Connect (OIDC) is an authentication layer built on top of the OAuth 2.0 framework. While OAuth 2.0 is for authorization, OIDC provides a way for clients to verify the identity of the end-user based on the authentication performed by an Authorization Server. It obtains basic profile information about the end-user in an ID Token (a JWT). It's what allows "Sign in with Google" to confirm who you are.

History: OIDC was created to address the lack of a standard authentication solution using OAuth 2.0. The OpenID Foundation, a non-profit organization, developed the specification and released the core documents in 2014. It has become the de facto standard for federated identity.

Standard Documents:

- OpenID Connect Core 1.0: The core specification defining the identity token, user info endpoint, and authentication flows.
- OpenID Connect Discovery 1.0: Defines a way for clients to discover the OpenID Provider's configuration.
- OpenID Connect Dynamic Client Registration 1.0: Defines how clients can register themselves with an OpenID Provider.

## SCIM

System for Cross-domain Identity Management (SCIM) is a REST-based protocol designed to automate the exchange of user and group identity information between different systems. Its purpose is to simplify user lifecycle management operations like creating, updating, and deleting user accounts across multiple applications. It uses a standard schema for users and groups, which makes integration between identity providers and service providers much easier.

History: The SCIM working group was formed by the Open Web Foundation in 2010, aiming to create an open standard for identity management. The group later moved to the IETF. The core specifications were published as RFCs in 2011.

Standard Documents:

- RFC 7642: SCIM - Definitions, Overview, Concepts, and Requirements
- RFC 7643: SCIM - Core Schema
- RFC 7644: SCIM - Protocol

## WebAuthn

Web Authentication (WebAuthn) is a web standard published by the World Wide Web Consortium (W3C) that allows web applications to implement strong, fido-based authentication. It enables users to authenticate using cryptographic keys stored on a secure hardware device called an authenticator (e.g., a hardware security key, a biometric reader, or a phone's built-in authenticator) instead of passwords. It's a key component of the FIDO2 project.

History: WebAuthn was developed jointly by the FIDO Alliance and the World Wide Web Consortium (W3C) to provide a web-native API for strong authentication. The first public draft was published in 2018, and it became a W3C Recommendation in 2019. It's widely supported by major web browsers.

Standard Documents:

- W3C Recommendation: Web Authentication: An API for accessing Public Key Credentials Level 2

## FIDO2

FIDO2 is a set of open standards that enable passwordless and phish-resistant authentication on the web. It's a collaboration between the FIDO Alliance and the W3C. The FIDO2 standard consists of two main components: WebAuthn (the web API) and the Client-to-Authenticator Protocol (CTAP). CTAP is a protocol that allows a web browser or operating system to communicate with an external authenticator, like a USB security key.  This combination allows for a secure, user-friendly, and standardized way to authenticate without passwords.

History: The FIDO Alliance was founded in 2012 to address the lack of interoperable authentication standards. The FIDO2 specifications were launched in 2018, building upon the earlier FIDO U2F and UAF protocols. FIDO2 is a major step toward a passwordless future.

Standard Documents:

- WebAuthn: W3C Recommendation (as listed above)
- CTAP: FIDO Alliance Specification: Client to Authenticator Protocol (CTAP) Version 2.1

