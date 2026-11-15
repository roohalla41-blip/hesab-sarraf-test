# ATLAS AI — Server API Contract

This document distinguishes the currently deployed chat endpoint from the authenticated API contract required for production.

## Current deployed endpoint
- Function: `atlas-chat`
- Project ref: `wwlyvojtsrklcpqsloqz`
- Last observed state: ACTIVE, version 9
- JWT verification: disabled (`verify_jwt=false`)
- Client callers: Android `SupabaseChatGateway` and browser `preview/app.js`
- Authentication: no user JWT is currently required
- Request body:
  - `message`: required string, maximum 3,000 characters
  - `history`: optional array of recent `user`/`assistant` entries, limited to the latest 8 by the function
  - `mode`: optional short string
- Response: `{ "reply": "...", "provider": "...", "model": "..." }`
- Errors: JSON `error` field with HTTP status such as 400, 403, 413, 429, 502, or 503
- Provider routing: server-side OpenAI-compatible, Groq, Z.AI, and Cloudflare Workers AI configurations
- Request controls: bounded message/history and in-memory IP throttling only

The public endpoint and ephemeral throttling are not sufficient as the only protections for a broad public launch. Provider secrets remain server-side, but requests are not currently tied to a verified user identity or durable usage quota.

## Backend identity gate
The configured Supabase project ref `wwlyvojtsrklcpqsloqz` has a public schema containing 59 tables whose names are associated with exchange-office accounting, users, wallets, transfers, rates, and payments. The read-only table listing reported zero rows at inspection. The database identity and data boundary must be confirmed before any Auth configuration, database schema, RLS, quota, or cloud-history change. No database or Auth changes were made during the review.

## Production API requirements
Before broad public release, the application backend must:
- Require a verified user JWT or another reviewed authentication mechanism.
- Enforce account identity and authorization on every user-owned resource.
- Own plan entitlements, durable quotas, atomic usage accounting, and abuse controls.
- Keep provider credentials and model routing exclusively server-side.
- Use durable rate limiting rather than Edge isolate memory as the only control.
- Provide request IDs, bounded payloads, upstream timeouts, cancellation, and safe errors.
- Avoid logging raw prompts, passwords, tokens, or sensitive content.
- Provide explicit data export and deletion flows if accounts or cloud history are introduced.

## Planned API surface
| Method | Path | Purpose |
|---|---|---|
| POST | /v1/chat/completions | Authenticated chat completion |
| GET | /v1/account | Read the authenticated account profile |
| GET | /v1/usage | Read server-authoritative quota and usage |
| GET | /v1/plans/current | Read subscription entitlements |
| GET | /v1/conversations | List cloud-synced conversations, if enabled |
| POST | /v1/files | Upload a validated attachment |
| DELETE | /v1/files/{id} | Delete an owned attachment |
| POST | /v1/feedback | Submit message feedback or a safety report |
| GET | /v1/privacy | Read privacy preferences |
| PUT | /v1/privacy | Update privacy preferences |

## Future chat request contract
The authenticated API should accept a request ID, conversation ID, assistant mode, bounded message list, and server-issued attachment references. The client must never select privileged provider credentials or bypass server-side entitlements.

## Deployment boundary
The `atlas-chat` Edge Function has been deployed. No database schema, user records, Auth configuration, or RLS policy was changed during the current preparation work. Any future database/Auth work requires confirmation that the selected Supabase project is the intended isolated ATLAS data boundary.
