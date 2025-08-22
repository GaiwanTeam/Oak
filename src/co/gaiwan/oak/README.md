# util

Unopinioted, pure-mechanism utilities. Like our internal standard library. These
namespaces should be so generic they could be spun off into their own utility
libraries. They should not depend on namespaces outside of `util` (they may use
third party libs).

# domain

Domain layer, each namespace represents one type of entity used by the system,
and presents operations over those entities, including CRUD into the postgres
DB. This is the main policy layer, used by API and command line implementations.
It should expose meaningful operations in the domain.

# app

Application harness and configuration

# system

Components that make up the system, and that are started by Makina

# apis

Individual API implementations, e.g. OAuth, JWKS, SCIM, OData, etc. 

