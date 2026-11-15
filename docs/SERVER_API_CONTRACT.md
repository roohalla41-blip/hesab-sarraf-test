# ATLAS AI — Server API Contract (Draft)

This document defines the authenticated backend contract. It does not deploy or configure a service.

## Current chat function
The repository contains a Supabase Edge Function at supabase/functions/chat/index.ts. It:
- Requires a valid Supabase JWT (verify_jwt remains enabled).
- Accepts POST requests with mode, optional conversation_id, and a bounded messages array.
- Validates supported modes, roles, message length, total context size, and that the last message is from the user.
- Applies an exact-origin allowlist for browser clients; native requests without an Origin header are supported.
- Routes to configured providers in the server-defined order and falls back when one is unavailable.
- Returns a user-safe response with request_id, provider, model, and optional token usage.

Supported provider configurations are described in docs/AI_ENGINE_IMPLEMENTATION_STATUS.md.

## Security requirements
- The mobile client must never contain AI provider keys, privileged database keys, or service-role credentials.
- Every protected request must be authenticated by the application backend.
- The backend is authoritative for account identity, plan entitlements, usage quotas, rate limits, model routing, and safety decisions.
- Secrets are stored only in the hosting provider's secret manager.
- Logs must not store passwords, access tokens, raw payment details, or unnecessary message contents.
- Requests must use TLS, bounded payload sizes, timeouts, and request identifiers.
- File uploads require type/size validation, private storage by default, and short-lived access URLs.
- User deletion and data export must have explicit, documented server workflows.

## Proposed application endpoints

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
- request_id: client-generated idempotency/correlation identifier.
- conversation_id: optional conversation identifier.
- mode: general, writing, learning, coding, or research.
- messages: bounded role/content entries.
- attachments: optional server-issued attachment references in a future version.

The client must not choose privileged provider credentials or bypass server-side entitlement checks.

## Operational behavior still required
- Android authentication and a secure session lifecycle.
- Android HTTP adapter and end-to-end tests.
- Streaming and cancellation propagation.
- Atomic per-account and per-IP rate limits.
- Atomic quota enforcement and usage ledger.
- Moderation, abuse prevention, and provider-health monitoring.
- Cloud conversation storage, export, and deletion.
- Explicit handling for unauthorized, quota-exceeded, rate-limited, unavailable, and invalid-input states.

## Deployment boundary
No remote Supabase project, database schema, authentication settings, RLS, or function deployment has been changed. Deployment requires explicit approval of the independent ATLAS project and the account-authentication approach.
