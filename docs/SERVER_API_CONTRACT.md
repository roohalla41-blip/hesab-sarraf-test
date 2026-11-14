# ATLAS AI — Server API Contract (Draft)

This document defines a provider-neutral contract for a future authenticated application backend. It does not deploy or configure a service.

## Security requirements
- The mobile client must never contain AI provider keys, privileged database keys, or service-role credentials.
- Every protected request must be authenticated by the application backend.
- The backend is authoritative for account identity, plan entitlements, usage quotas, rate limits, model routing, and safety decisions.
- Secrets are stored only in the hosting provider's secret manager.
- Logs must not store passwords, access tokens, raw payment details, or unnecessary message contents.
- Requests must use TLS, bounded payload sizes, timeouts, and request identifiers.
- File uploads require type/size validation, private storage by default, and short-lived access URLs.
- User deletion and data export must have explicit, documented server workflows.

## Proposed endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | /v1/chat/completions | Send conversation context and receive an assistant response |
| GET | /v1/account | Read the authenticated account profile |
| GET | /v1/usage | Read server-authoritative quota and usage |
| GET | /v1/plans/current | Read the current subscription entitlement |
| GET | /v1/conversations | List cloud-synced conversations when sync is enabled |
| POST | /v1/files | Upload a validated attachment |
| DELETE | /v1/files/{id} | Delete an owned attachment |
| POST | /v1/feedback | Submit message feedback or a safety report |
| GET | /v1/privacy | Read privacy preferences |
| PUT | /v1/privacy | Update privacy preferences |

## Chat request shape

A future request should include:
- request_id: client-generated idempotency/correlation identifier.
- conversation_id: optional conversation identifier.
- mode: a product mode such as general, learning, or research.
- messages: bounded role/content entries.
- attachments: optional server-issued attachment references.

The client must not choose privileged provider credentials or bypass server-side entitlement checks.

## Chat response shape

A successful response should include:
- request_id
- conversation_id
- message_id
- status
- assistant_message
- optional safe usage metadata

Errors should use stable machine-readable codes and a user-safe message. Do not expose provider stack traces, secret names, or internal infrastructure details.

## Operational behavior
- Support cancellation and reasonable request timeouts.
- Apply per-account and per-IP rate limits.
- Enforce quotas atomically on the server to prevent parallel-request overuse.
- Use bounded retries only for retryable failures and preserve idempotency.
- Return explicit states for unauthorized, quota-exceeded, rate-limited, unavailable, and invalid-input conditions.

## Not implemented yet
No endpoint in this document is live. No database schema, authentication provider, billing provider, AI model, or production hosting environment has been selected or modified.
