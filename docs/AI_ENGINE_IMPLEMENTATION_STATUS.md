# ATLAS AI — AI Engine Implementation Status

This is a repository-only implementation. It does not deploy or configure Supabase or an external provider.

## Implemented in this repository
- Provider-neutral Android gateway boundary with explicit failure states.
- Supabase Edge Function source for the protected chat endpoint; JWT verification remains enabled.
- Server-side provider routing and fallback order: Z.AI, Cloudflare Workers AI, and a generic OpenAI-compatible provider.
- Provider credentials are read only from server environment variables; no provider secret is embedded in Android or committed to the repository.
- Validation for supported modes, message roles, per-message size, total context size, and final user turn.
- Request correlation IDs, bounded per-provider timeout, HTTPS-only provider endpoint configuration, and stable user-safe error codes.
- Optional exact-origin allowlist through ATLAS_ALLOWED_ORIGINS; native requests without an Origin header are supported.
- Provider response parsing with optional token usage metadata.
- GitHub Actions now type-checks the Edge Function with Deno and runs Android unit tests plus debug package validation.

## Provider environment configuration
- Z.AI: ZAI_API_KEY, optional ZAI_MODEL (defaults to glm-5.3), optional ZAI_API_URL (defaults to the standard chat-completions endpoint).
- Cloudflare Workers AI: CLOUDFLARE_API_TOKEN or ATLAS_CLOUDFLARE, plus CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_MODEL.
- Generic OpenAI-compatible provider: AI_API_KEY, AI_MODEL, and optional AI_API_URL (base URL; defaults to the OpenAI v1 base).
- AI_PROVIDER_ORDER optionally selects the fallback order, for example zai,cloudflare,openai.
- All values must be configured in the Supabase Function Secret Manager only. Do not put them in GitHub source, Android BuildConfig, or the APK.

## Not implemented
- No provider secrets have been deployed or tested against a live model.
- An authenticated Android HTTP adapter exists and validates request bounds, but it is not wired into the UI because no approved account session or project configuration is available.
- No Supabase project has been linked or deployed from this repository.
- No account authentication flow is implemented in Android; the function's JWT requirement therefore prevents unauthenticated app calls.
- No streaming/SSE or Android cancellation propagation.
- No atomic per-account quota, per-IP rate limiting, abuse controls, moderation pipeline, or usage ledger.
- No cloud conversation persistence or account lifecycle.
- No file, image, audio, web, or coding tool execution.
- No production monitoring or live integration tests.

## Security and rollout gates
1. Confirm the independent ATLAS Supabase project and exact deployment target. Do not use the protected Sarraf database.
2. Decide the account sign-in method and verify JWT claims and ownership rules.
3. Configure provider secrets only in the server Secret Manager; never request or commit secret values.
4. Review provider order, model availability, account permissions, and billing/credits.
5. Add an atomic quota/rate-limit store before public rollout; in-memory counters are not sufficient in serverless environments.
6. Set ATLAS_ALLOWED_ORIGINS to exact trusted web origins before browser access.
7. Add Android adapter and end-to-end integration tests, then explicitly authorize deployment. This source change does not deploy anything.

## Current status
The provider fallback source is implemented and is included in static type-checking, but the AI engine is not connected to Android or a live Supabase project. The app must not be described as a functioning general AI assistant until authentication, secrets, deployment, and real-device tests are complete.
