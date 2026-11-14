# ATLAS AI — AI Engine Implementation Status

This is a repository-only implementation draft. It does not deploy or configure Supabase or an external provider.

## Implemented in this repository
- Provider-neutral Android gateway boundary with explicit failure states.
- Server-side chat function draft reads provider credentials only from server environment variables.
- Validation for supported modes, message roles, per-message size, total context size, and final user turn.
- Request correlation IDs, bounded upstream timeout, HTTPS-only provider endpoint configuration, and stable user-safe error codes.
- Optional exact-origin allowlist through ATLAS_ALLOWED_ORIGINS; native requests without an Origin header are supported.
- Provider response parsing with optional token usage metadata.
- Supabase function source configuration keeps JWT verification enabled; it has not been deployed.

## Not implemented
- No live model is configured; no API key is present.
- No Android HTTP adapter is connected to the Edge Function.
- No streaming/SSE or Android cancellation propagation.
- No atomic per-account quota, per-IP rate limiting, abuse controls, moderation pipeline, or usage ledger.
- No account authentication flow or persistent conversation storage.
- No file, image, audio, web, or coding tool execution.
- No production monitoring or integration tests.

## Security and rollout gates
1. Approve the hosting project and provider separately.
2. Review authentication claims and ownership rules before enabling requests.
3. Add an atomic quota/rate-limit store before public rollout; in-memory counters are not sufficient in serverless environments.
4. Configure AI_API_KEY, AI_MODEL, and optionally AI_API_URL only in the server Secret Manager. Never commit values or place them in Android.
5. Set ATLAS_ALLOWED_ORIGINS to exact trusted web origins before browser access.
6. Add adapter and integration tests, then explicitly authorize deployment. This change does not deploy anything.

## Current status
The server-side request boundary is more fully specified and validated, but the AI engine is not complete or connected. Review and test this source draft before deployment.