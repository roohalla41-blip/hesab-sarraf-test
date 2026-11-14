# ATLAS AI — AI Engine Implementation Status

This is a repository-only implementation draft. It does not deploy or configure Supabase or an external provider.

## Implemented in this repository
- Provider-neutral Android gateway boundary with explicit failure states.
- Configurable Android HTTP adapter for the existing Persian JSON contract of the legacy `ai-assistant` function.
- Adapter keeps provider credentials out of Android and supports an optional app-session token.
- Server-side chat function draft reads provider credentials only from server environment variables.
- Validation for supported modes, message roles, per-message size, total context size, and final user turn.
- Request correlation IDs, bounded upstream timeout, HTTPS-only provider endpoint configuration, and stable user-safe error codes.
- Optional exact-origin allowlist through ATLAS_ALLOWED_ORIGINS; native requests without an Origin header are supported.
- Provider response parsing with optional token usage metadata.
- Supabase function source configuration keeps JWT verification enabled; it has not been deployed.

## Not implemented
- The Android adapter is not wired into MainActivity or a ViewModel and is not enabled in the app.
- The currently deployed `ai-assistant` function still contains the legacy حساب صراف system prompt and reads `GROQ_API_KEY` / `GROQ_MODEL`; it is not an ATLAS production endpoint.
- No confirmed successful live model request from the Android app.
- No streaming/SSE or Android cancellation propagation.
- No atomic per-account quota, durable per-IP rate limiting, abuse controls, moderation pipeline, or usage ledger.
- No account authentication flow or persistent conversation storage.
- No file, image, audio, web, or coding tool execution.
- No production monitoring or automated integration tests.

## Security and rollout gates
1. Keep the legacy `ai-assistant` endpoint unchanged until a separate ATLAS endpoint and migration plan are approved.
2. Review authentication claims and ownership rules before enabling requests. Do not rely on an app-embedded secret or a publishable key as user authentication.
3. Add an atomic quota/rate-limit store before public rollout; in-memory counters are not sufficient in serverless environments.
4. Configure provider credentials only in the server Secret Manager. Never commit values or place them in Android.
5. Set exact trusted web origins before browser access.
6. Add adapter and integration tests, then explicitly authorize deployment. This change does not deploy anything.

## Current status
The Android transport adapter has been added as an isolated, configurable component on a work branch, but it is not connected to the UI or a dedicated ATLAS backend. The production AI engine is not complete. Review and test all source before deployment.
