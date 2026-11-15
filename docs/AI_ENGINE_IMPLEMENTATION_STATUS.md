# ATLAS AI — AI Engine Implementation Status

## Deployed service path
- Configured Supabase project ref: `wwlyvojtsrklcpqsloqz`.
- Edge Function `atlas-chat`: ACTIVE, version 9 at the last read-only inspection.
- Android chat calls `/functions/v1/atlas-chat` through `SupabaseChatGateway`.
- Browser chat in `preview/app.js` calls the same endpoint.
- The clients send the public publishable key; no provider secret is embedded in Android or browser source.
- The function allows the published GitHub Pages origin and native requests without an Origin header.

## Provider routing
The deployed function can try configured providers in this order:
1. Generic OpenAI-compatible endpoint: `AI_API_KEY`, `AI_API_URL`, `AI_MODEL`.
2. Groq: `GROQ_API_KEY`, optional `GROQ_API_URL`, optional `GROQ_MODEL`.
3. Z.AI: `ZAI_API_KEY`, `ZAI_MODEL`, optional `ZAI_API_URL`.
4. Cloudflare Workers AI: `ATLAS_CLOUDFLARE` or `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_MODEL`.

A provider is usable only when all required server-side values are present. Secret values are not exposed in client code or documentation.

## Security and reliability status
- The deployed function currently has JWT verification disabled (`verify_jwt=false)); requests do not require an authenticated ATLAS user.
- The function applies per-IP request limits in Edge isolate memory. These limits are best-effort, resettable, and are not durable quotas.
- There is no durable per-user usage accounting, account lifecycle, or persistent cloud chat history.
- A public endpoint and ephemeral IP limits are not sufficient as the only controls for a broad public launch.
- The configured Supabase project currently contains 59 public tables whose names relate to exchange-office accounting, users, wallets, transfers, rates, and payments. The reported row counts were zero at inspection. This database identity/data boundary must be confirmed before any Auth, schema, RLS, quota, or cloud-history changes. No database, Auth, or RLS changes were made during this inspection.

## Implemented in source / deployment
- Android and browser clients send general chat messages to the deployed endpoint.
- Pending responses are replaced by a model answer or a user-facing connection error.
- Conversation history is bounded before it is forwarded to the model.
- Server-side provider fallback, bounded upstream timeout, safe error messages, and no-store responses.
- Gmail remains a separate read-only Android integration.

## Not yet verified or implemented
- No live model request has been made to confirm which provider secrets and model configuration are currently valid.
- No Android build, emulator test, or physical-device test was run as part of the latest interface and endpoint changes.
- User authentication/JWT verification, durable quotas, abuse-resistant rate limiting, and persistent cloud history are not implemented.
- Gmail OAuth consent and real-device validation remain outstanding.
- File, image, audio, web search, and side-effecting integrations are not live tools.
- Browser history is session-only; Android history is local to the device.

## Release note
The chat clients and Edge Function endpoint are connected at the source/deployment level. This is not proof of a successful live model response, secure public-user authentication, or full product readiness. No Supabase database schema, user records, Auth configuration, or RLS policy was changed during the inspection.
