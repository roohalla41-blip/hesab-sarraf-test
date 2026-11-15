# ATLAS AI — AI Engine Implementation Status

## Live service connection
- Supabase project: `wwlyvojtsrklcpqsloqz` (the independent ATLAS project).
- Deployed function: `atlas-chat`, active deployment version 9.
- Android chat now calls `/functions/v1/atlas-chat` through `SupabaseChatGateway`.
- The browser client in `preview/app.js` calls the same live function.
- The Android client sends the project's public publishable key in the `apikey` header. No AI provider secret is embedded in the APK or browser code.
- The function currently permits the published GitHub Pages origin and native Android requests without an Origin header.
- The function keeps its public endpoint mode (`verify_jwt = false`) and applies per-IP request limits in Edge isolate memory. These limits are best-effort and are not a durable quota system.

## Provider configuration
The function tries configured providers in this order:
1. Generic OpenAI-compatible endpoint: `AI_API_KEY`, `AI_API_URL`, and `AI_MODEL`.
2. Groq: `GROQ_API_KEY`, optional `GROQ_API_URL`, and optional `GROQ_MODEL`.
3. Z.AI: `ZAI_API_KEY`, `ZAI_MODEL`, and optional `ZAI_API_URL`.
4. Cloudflare Workers AI: `ATLAS_CLOUDFLARE` or `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`, and `CLOUDFLARE_MODEL`.

All provider values must be stored in the Supabase Edge Function Secret Manager. Never place them in Android source, browser JavaScript, GitHub, or the APK. A provider is usable only when its required secret and model configuration exist.

## Implemented
- Android sends general chat requests to the deployed ATLAS endpoint and replaces the pending response with the model answer or a specific connection error.
- Browser chat sends requests to the same endpoint and renders assistant replies and service errors.
- Conversation context is passed with bounded message count and length.
- Server-side provider fallback, bounded upstream timeout, safe error messages, request limits, and no-store responses.
- Gmail remains a separate read-only Android integration.

## Remaining before broad public release
- No Android build, emulator test, or physical-device test was run for this change, as requested.
- No live model request was made to verify which provider secret is currently populated.
- User accounts, verified JWTs, durable per-user quotas, persistent cloud conversation storage, and abuse-resistant rate limiting are not implemented.
- Edge-isolate in-memory limits can reset and are not sufficient as the only control for a large public launch.
- Gmail OAuth consent, production verification, and real-device validation remain separate release requirements.
- File, image, audio, web search, and side-effecting integrations are not implemented as live tools.
- The browser history is session-only; Android history is local to the device.

## Release notes
The AI chat path is now connected in source and the Supabase Edge Function has been deployed. This does not mean every ATLAS feature is complete or that a signed Android release has been produced. No database schema, Auth configuration, RLS policy, or user record was changed as part of this connection.
