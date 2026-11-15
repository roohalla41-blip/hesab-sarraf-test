# ATLAS AI — Application Architecture

## Product direction
ATLAS AI is a multilingual assistant with a Persian/Dari-first experience and English support. Android is the primary application; the browser version provides a web interface.

## Architecture boundaries
- **app/core** — application configuration, locale policy, and shared foundations.
- **domain/model** — provider-independent chat and product models.
- **domain/repository** — contracts for chat and history.
- **domain/usecase** — application actions and orchestration.
- **data/remote** — HTTPS adapter for the ATLAS Edge Function.
- **data/local** — private on-device conversation storage (currently SharedPreferences JSON; transactional database migration remains a release task).
- **feature/** — product areas and UI state.
- **preview/** — browser interface and client-side interaction.

## Current AI request path
1. Android and browser clients submit a message to the Supabase Edge Function `atlas-chat`.
2. The function validates request shape and size, bounds history, and routes to configured server-side providers.
3. Provider credentials are read from Edge Function secrets and never belong in the client.
4. The function returns a model response or a safe error message.

The function was observed as ACTIVE, version 9, with JWT verification disabled. This is a public unauthenticated endpoint. Its current in-memory IP limits are best-effort and can reset between Edge isolates; they are not a durable quota or abuse-prevention system. A verified user-authentication design and durable quotas are release blockers.

## Backend identity and data boundary
The configured Supabase project ref is `wwlyvojtsrklcpqsloqz`. A read-only inspection found 59 public tables whose names and relationships appear related to exchange-office accounting, users, wallets, transfers, rates, and payments; all reported row counts were zero at inspection. The project identity and data boundary must be explicitly confirmed before any Auth, schema, RLS, quota, or cloud-history changes. No database, Auth, or RLS changes were made during inspection.

## Local data
- Android retains up to 100 conversations and up to 300 messages per conversation in app-private storage.
- The local store keeps a previous snapshot for limited recovery.
- Browser conversation history is session-only and is not synchronized to Android.
- Cloud sync, accounts, export, and deletion workflows are not implemented.

## Product status boundaries
- Gmail read-only code exists but OAuth and physical-device behavior remain unverified.
- File, image, voice, web research, coding tools, account management, subscriptions, and billing are not live capabilities yet.
- UI availability labels must distinguish working features from planned integrations.
- A successful build alone does not establish production readiness.
