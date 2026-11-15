# ATLAS CONNECT — Integration architecture

## Gmail: read-only Android integration

The Android app contains a Gmail read-only integration path.

### Implemented in source
- Google Identity Services AuthorizationClient requests the minimum Gmail read-only scope.
- After consent, ATLAS requests up to 8 inbox messages and displays sender, subject, date metadata, and snippet.
- The Integrations page tracks authorization state separately from inbox contents.
- Direct Gmail-check phrases in Persian or English can invoke this Gmail-specific path and append the result to the local conversation.
- Gmail snippets are stored with the local chat history in app-private SharedPreferences. This storage is not protected by a dedicated encrypted-at-rest layer; do not use a shared/public device for sensitive mailbox testing.
- The access token is held in Activity memory only, not written to SharedPreferences, Supabase, or GitHub.
- The integration is read-only: it does not send, draft, delete, modify, or archive email.

### Limitations
- Gmail OAuth has not yet been verified on a physical device.
- Google access tokens are short-lived; server-side offline access would require a separate secure refresh-token architecture.
- Gmail-check phrases are a narrow command path, not general semantic reasoning over a mailbox.
- Disconnecting the app's in-memory session does not revoke the Google consent grant.
- Public Gmail user-data scopes may require Google's OAuth verification and additional compliance steps.

## Google Cloud setup before device validation
1. Create or select a Google Cloud project.
2. Enable Gmail API.
3. Configure OAuth consent and test users if the consent screen is in Testing mode.
4. Create an Android OAuth client for package `com.hooshmasnooi.app` and the SHA-1 fingerprint of the installed build's signing certificate.
5. Install a candidate build signed by the matching certificate and add its SHA-1 to the Android OAuth client.
6. Connect Gmail from ATLAS CONNECT and approve read-only access.
7. Confirm that inbox messages load and that the Gmail-check chat command returns a summary.

GitHub Actions is configured to run Android unit tests and assemble a debug package on pushes to `main`; the latest run result has not been reviewed as part of this documentation update.

## General AI chat
- Android and browser clients call the deployed `atlas-chat` function in configured Supabase project ref `wwlyvojtsrklcpqsloqz`.
- The function supports generic OpenAI-compatible, Groq, Z.AI, and Cloudflare Workers AI routes when their server-side secrets and model settings exist.
- The function currently accepts requests without a user JWT (`verify_jwt=false`) and uses best-effort in-memory IP throttling. Authentication and durable quotas remain release blockers.
- Provider secrets must remain in Edge Function Secret Manager and never appear in Android, APK, browser JavaScript, or GitHub.

## Backend data boundary warning
A read-only inspection of the configured Supabase project found 59 public tables with names associated with exchange-office accounting, users, wallets, transfers, rates, and payments; all reported row counts were zero at inspection. Confirm the project and data boundary before adding Auth, persistent chat history, usage tables, or quota controls. No database schema, records, Auth settings, or RLS policies were changed during this review.

## Other integrations
- GitHub, Drive, and Calendar are directory entries only; OAuth, per-user credential storage, provider calls, and validation remain unimplemented.
- File, image, voice, web research, and coding tools are not live integrations.

## Security boundaries
- Never embed OAuth client secrets, refresh tokens, service-role keys, or provider API secrets in Android or browser code.
- Never store Gmail tokens in a shared global secret or shared user record.
- Any future server-side token storage must be per-user, encrypted, access-controlled, revocable, and linked to a verified app user.
- Keep Gmail scopes minimal and request permission only when the user invokes the feature.
- Side-effecting actions require a separate implementation review and explicit user confirmation.

## Official references
- Android authorization: https://developer.android.com/identity/authorization
- AuthorizationClient API: https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationClient
- Gmail OAuth scopes: https://developers.google.com/workspace/gmail/api/auth/scopes
- OAuth verification: https://developers.google.com/identity/protocols/oauth2/production-readiness/restricted-scope-verification
