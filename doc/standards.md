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
- RFC 6819: OAuth 2.0 Threat Model and Security Considerations
- RFC 7009: OAuth 2.0 Token Revocation
- RFC 7591: OAuth 2.0 Dynamic Client Registration Protocol
- RFC 7592: OAuth 2.0 Dynamic Client Management Protocol 
- RFC 7636: Proof Key for Code Exchange (PKCE) by OAuth Public Clients
- RFC 7662: OAuth 2.0 Token Introspection specification 
- RFC 8414: OAuth 2.0 Authorization Server Metadata
- RFC 8628: OAuth 2.0 Device Authorization Grant
- RFC 8693: OAuth 2.0 Token Exchange
- RFC 8705: OAuth 2.0 Mutual-TLS Client Authentication and Certificate-Bound Access Tokens (mTLS)
- RFC 8252: OAuth 2.0 for Native Apps
- RFC 9068: JSON Web Token (JWT) Profile for OAuth 2.0 Access Tokens
- RFC 9101: The OAuth 2.0 Authorization Framework: JWT-Secured Authorization Request (JAR)
- RFC 9126: OAuth 2.0 Pushed Authorization Requests (PAR)
- RFC 9207: OAuth 2.0 Step-up Authentication Challenge Protocol
- RFC 9396: OAuth 2.0 Rich Authorization Requests
- RFC 9449: OAuth 2.0 Demonstrating Proof of Possession (DPoP)
- RFC 9700: Best Current Practice for OAuth 2.0 Security

Drafts:

- [OAuth 2.0 for Browser-Based Applications](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-browser-based-apps)
- [The OAuth 2.1 Authorization Framework](https://datatracker.ietf.org/doc/draft-ietf-oauth-v2-1/)
- [OAuth 2.0 for First-Party Applications](https://datatracker.ietf.org/doc/draft-ietf-oauth-first-party-apps/)

## OIDC

OpenID Connect (OIDC) is an authentication layer built on top of the OAuth 2.0 framework. While OAuth 2.0 is for authorization, OIDC provides a way for clients to verify the identity of the end-user based on the authentication performed by an Authorization Server. It obtains basic profile information about the end-user in an ID Token (a JWT). It's what allows "Sign in with Google" to confirm who you are.

History: OIDC was created to address the lack of a standard authentication solution using OAuth 2.0. The OpenID Foundation, a non-profit organization, developed the specification and released the core documents in 2014. It has become the de facto standard for federated identity.

Standard Documents:

- OpenID Connect Core 1.0: The core specification defining the identity token, user info endpoint, and authentication flows.
- OpenID Connect Discovery 1.0: Defines a way for clients to discover the OpenID Provider's configuration.
- OpenID Connect Dynamic Client Registration 1.0: Defines how clients can register themselves with an OpenID Provider.
- [OpenID Connect Session Management 1.0](https://openid.net/specs/openid-connect-session-1_0.html)
- [OpenID Connect Front-Channel Logout 1.0](https://openid.net/specs/openid-connect-frontchannel-1_0.html)
- [OpenID Connect Back-Channel Logout 1.0 incorporating errata set 1](https://openid.net/specs/openid-connect-backchannel-1_0.html)
- [OpenID Connect Client-Initiated Backchannel Authentication Flow - Core 1.0](https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html)
- [OpenID Federation 1.0 - draft 43](https://openid.net/specs/openid-federation-1_0.html)
- [Self-Issued OpenID Provider v2 - draft 13](https://openid.net/specs/openid-connect-self-issued-v2-1_0.html)
- [OpenID for Verifiable Presentations 1.0](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html)

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

## UMA

- [User-Managed Access (UMA) 2.0 Grant for OAuth 2.0 Authorization](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.html)
- [Federated Authorization for User-Managed Access (UMA) 2.0](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-federated-authz-2.0.html)

## DID (Distributed ID) and VC (Verifiable Credentials)

- [Decentralized Identifiers (DIDs) v1.1](https://www.w3.org/TR/did-1.1/)
- [Verifiable Credentials Data Model v2.0](https://www.w3.org/TR/vc-data-model-2.0/)
- [SD-JWT-based Verifiable Credentials (SD-JWT VC)](https://datatracker.ietf.org/doc/draft-ietf-oauth-sd-jwt-vc/)

## SPIFFE

See [SPIFFE standards (github)](https://github.com/spiffe/spiffe/tree/main/standards)

- [The SPIFFE Identity and Verifiable Identity Document](https://github.com/spiffe/spiffe/blob/main/standards/SPIFFE-ID.md)
- [The X.509 SPIFFE Verifiable Identity Document](https://github.com/spiffe/spiffe/blob/main/standards/X509-SVID.md)
- [The JWT SPIFFE Verifiable Identity Document](https://github.com/spiffe/spiffe/blob/main/standards/JWT-SVID.md)
- [The SPIFFE Workload API](https://github.com/spiffe/spiffe/blob/main/standards/SPIFFE_Workload_API.md)

## SAML

Security Assertion Markup Language (SAML) is an XML-based framework for exchanging authentication and authorization data between an identity provider (IdP) and a service provider (SP). Its primary use case is single sign-on (SSO), allowing a user to authenticate once with an IdP and then access multiple SPs without re-entering credentials. It's often used in enterprise environments to connect corporate identity systems to cloud applications.

SAML was developed by the OASIS Security Services Technical Committee, with the first version, SAML 1.0, published in 2002. It was designed to address the need for a standardized, cross-domain SSO solution for web applications. The most widely adopted version is SAML 2.0, published in 2005. While newer protocols like OIDC are gaining popularity, SAML remains a dominant standard, especially for enterprise SSO.

Standard Documents:

- OASIS Standard: Assertions and Protocols for the OASIS Security Assertion Markup Language (SAML) V2.0
- OASIS Standard: Bindings for the OASIS Security Assertion Markup Language (SAML) V2.0
- OASIS Standard: Profiles for the OASIS Security Assertion Markup Language (SAML) V2.0
Of course. Your overview is excellent and covers the most critical modern identity and access management standards. To make it more complete, you could add technologies that represent foundational pillars (especially in enterprise environments), advanced authorization models, and key security profiles.

Here are some documents and technologies that would round out the overview:

## Kerberos

Kerberos is a network authentication protocol designed to provide strong authentication for client/server applications by using secret-key cryptography. It works on the basis of "tickets" to allow nodes communicating over a non-secure network to prove their identity to one another in a secure manner. It is the core authentication protocol used by Microsoft Active Directory. ️

History: Kerberos was developed at the Massachusetts Institute of Technology (MIT) in the 1980s as part of Project Athena. Its purpose was to protect information services distributed across a campus network. It has since become a widely adopted IETF standard.

Standard Documents:
* RFC 4120: The Kerberos Network Authentication Service (V5)

## LDAP (Lightweight Directory Access Protocol)

LDAP is an open, vendor-neutral application protocol for accessing and maintaining distributed directory information services over an IP network. Directories are a critical component of any identity system, acting as the primary store for user, group, and attribute information. Systems like Microsoft Active Directory and OpenLDAP are based on this protocol.

History: LDAP was created by Tim Howes at the University of Michigan in the early 1990s as a lightweight front-end to the heavier X.500 directory services protocols. It was quickly adopted by the IETF and has become the de facto standard for directory access.

Standard Documents:
* RFC 4511: Lightweight Directory Access Protocol (LDAP): The Protocol
* RFC 4512: Lightweight Directory Access Protocol (LDAP): Directory Information Models

## PKI / X.509

Public Key Infrastructure (PKI) is a set of roles, policies, hardware, software, and procedures needed to create, manage, distribute, use, store, and revoke digital certificates and manage public-key encryption. The core of PKI is the X.509 certificate, a standard defining the format of public-key certificates. These certificates are used everywhere, from securing websites with TLS/SSL to signing code, JWTs (using `x5t` headers), and SAML assertions. 

History: The X.509 standard was first issued in 1988 as part of the X.500 standard. Its adoption for securing web traffic with SSL (now TLS) in the mid-1990s cemented its role as a foundational technology for internet security. The Internet Engineering Task Force (IETF) maintains the profile for its use on the internet.

Standard Documents:
* RFC 5280: Internet X.509 Public Key Infrastructure Certificate and Certificate Revocation List (CRL) Profile

## Financial-grade API (FAPI)

The Financial-grade API (FAPI) is a high-security profile of OAuth 2.0 and OIDC. It specifies a set of strict security and interoperability guidelines for applications that handle sensitive financial or personal data. It mandates stronger authentication, stricter client validation, and more robust token-binding mechanisms (like mTLS). While it originated in open banking, its principles are now being applied in healthcare and other high-stakes industries. 

History: FAPI was developed by the OpenID Foundation's FAPI Working Group to meet the security requirements of regulations like the UK's Open Banking and Europe's Payment Services Directive (PSD2). The first parts were finalized around 2019.

Standard Documents:
* FAPI 1.0 - Part 1: Read-Only API Security Profile
* FAPI 1.0 - Part 2: Read and Write API Security Profile
* FAPI 2.0: The next generation, simplifying the standard for broader adoption.

## XACML (eXtensible Access Control Markup Language)

XACML is an OASIS standard that describes a policy language and an access control decision model. While OAuth 2.0 and OIDC handle authentication and delegated authorization, XACML is designed for fine-grained, attribute-based access control (ABAC). It provides a standardized way to define complex authorization policies (e.g., "Allow doctors to view the medical records of patients in their own department during business hours") and a request/response protocol for evaluating them.

History: XACML was developed by the OASIS XACML Technical Committee. Version 1.0 was ratified in 2003, with the most recent major version, 3.0, being standardized in 2013. It is a powerful but often complex standard used in enterprise and government systems.

Standard Documents:

* OASIS Standard: eXtensible Access Control Markup Language (XACML) V3.0
