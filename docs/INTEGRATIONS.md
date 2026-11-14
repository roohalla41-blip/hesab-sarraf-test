# ATLAS CONNECT — Integration architecture

## Current prototype
The Android app and web preview include an **اتصال برنامه‌ها / ATLAS CONNECT** directory with GitHub, Gmail, Google Drive, Supabase, and Google Calendar entries. The user can inspect each provider's connection prerequisites. All providers are deliberately shown as **not connected**.

This is a working UI prototype only. It does not perform OAuth, request provider permissions, store credentials, or call external provider APIs.

## Secure implementation path
1. Register an OAuth application separately for each provider and configure exact redirect URIs.
2. Start authorization from the app using the provider's supported OAuth flow (Authorization Code + PKCE where supported).
3. Complete code exchange and token refresh only on a trusted backend. Never embed client secrets, provider access tokens, refresh tokens, or service-role keys in the Android APK or web JavaScript.
4. Store per-user encrypted credentials in a dedicated server-side secret/credential store. Do not store user OAuth tokens in a shared global environment secret.
5. Expose narrow server actions for each integration. Enforce user identity, scopes, ownership, rate limits, audit events, and token revocation.
6. Require explicit confirmation for side-effecting actions such as sending email, deleting files, or modifying repositories.
7. Provide disconnect/revoke controls and delete stored credentials on disconnect.

## First practical integration
GitHub is the recommended first integration because the app can demonstrate repository listing and read-only file inspection before adding write permissions. Start with minimum read-only scopes. Add write scopes only after a separate review and explicit approval.

## Current blockers
- No OAuth client IDs, redirect URIs, or provider applications have been registered for ATLAS.
- No authenticated integration backend or per-user credential store is implemented.
- No provider connection has been tested.
- Supabase production functions, database tables, authentication, and user records have not been changed as part of this UI prototype.

## Test checklist before claiming a connection works
- OAuth consent and callback succeed on Android and web.
- Tokens are absent from app logs, local preferences, GitHub, and client responses.
- Only the selected user's resources are accessible.
- Revocation/disconnect invalidates stored credentials.
- Read-only scopes cannot perform write actions.
- Side effects require confirmation and are audited.
