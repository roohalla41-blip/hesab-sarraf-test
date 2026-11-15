# ATLAS CONNECT — Integration architecture

## Gmail: first practical Android integration

The Android app has a Gmail read-only integration path in source code, not just a decorative connection card.

### What is implemented
- Google Identity Services AuthorizationClient requests the minimum scope currently needed: https://www.googleapis.com/auth/gmail.readonly.
- After the user grants consent, ATLAS calls the Gmail REST API and fetches up to 8 messages from the inbox, including sender, subject, date metadata, and snippet.
- The Integrations page displays fetched messages and tracks authorization state separately from inbox contents (an empty inbox can still show as connected).
- The main chat recognises direct Gmail-check requests in Persian or English and calls the same Gmail API path. The result is written back as an assistant message in the local conversation.
- The chat history, including Gmail snippets returned in chat, is stored locally in the app's private SharedPreferences. This current storage is app-private but is not encrypted at rest by a dedicated encrypted-storage layer; avoid using a shared/public device for sensitive mailbox testing.
- The access token is held in Activity memory only. It is not written to SharedPreferences, Supabase, or GitHub.
- If the Activity has no in-memory token (for example after app restart), a Gmail-check request from chat starts the Google authorization flow itself. If the user previously granted the scope and has not revoked it, Google can issue a fresh short-lived access token without asking again; otherwise the user is prompted to grant access.
- The current pilot is read-only: it does not send, draft, delete, modify, or archive email.

### Important limitations
- The latest source has passed the repository Android build validation, but Gmail has not yet been installed, authorized, and verified on a physical device.
- The Android authorization flow can request a fresh access token again while the user's Google grant remains active. Google access tokens are short-lived; server-side offline access would require a separate secure refresh-token architecture.
- The current chat understands a narrow set of explicit Gmail-check phrases. It is not yet a general natural-language AI agent and does not connect arbitrary Gmail content to an LLM for semantic reasoning.
- “Disconnect this session” removes the in-memory token from this Activity. It does not by itself revoke the Google consent grant; revoke ATLAS AI from the user's Google Account security settings to remove the grant.
- A public release using Gmail user-data scopes may require Google's OAuth verification and additional compliance steps. Development/testing and personal-use exceptions depend on Google's current rules.

## Google Cloud setup required before the first device test
1. Create or select a Google Cloud project.
2. Enable the Gmail API for that project.
3. Configure the OAuth consent screen and add the testing Google account as a test user if the app is in Testing mode.
4. Create an Android OAuth client using the exact package name com.hooshmasnooi.app and the SHA-1 fingerprint of the signing certificate used for the installed app. Debug and release certificates have different fingerprints.
5. The repository workflow now runs tests and validates the package internally; it does not publish or upload a downloadable APK artifact while development is in progress.
6. Before the final device test, create the approved signed candidate using the matching certificate, then add its SHA-1 to the Android OAuth client.
7. Open Connected apps, connect the Google account, and approve read-only Gmail access.
8. Confirm that messages load in the integration page, then send the Gmail-check command in chat and confirm that the inbox summary is returned.

## Other integrations
- The deployed `atlas-chat` Edge Function in the independent ATLAS project (`wwlyvojtsrklcpqsloqz`) supports a generic OpenAI-compatible endpoint, Groq, Z.AI, and Cloudflare Workers AI when their server-side secrets/model settings exist.
- Android and the browser client now call `atlas-chat`. The endpoint currently allows the published GitHub Pages origin and native Android requests without an Origin header; it uses a publishable project key and keeps provider secrets server-side.
- The endpoint currently operates in guest/public mode (`verify_jwt = false`) with best-effort in-memory IP throttling. Durable per-user quotas and account authentication remain outstanding.
- GitHub, Google Drive, and Google Calendar remain directory entries only. Their OAuth flows, per-user credential storage, provider API calls, and tests have not been implemented.

## Security boundaries
- Never embed OAuth client secrets, refresh tokens, service-role keys, or provider API secrets in the Android APK or web JavaScript.
- Do not store user Gmail tokens in a shared global environment secret or a shared database record.
- Any future server-side token storage must be per-user, encrypted, access-controlled, revocable, and tied to a verified app user.
- Keep Gmail scopes minimal and request permissions only when the user invokes the feature.
- Side-effecting actions such as sending or deleting email must require a separate implementation review and explicit user confirmation.
- The ATLAS `atlas-chat` Edge Function was deployed/updated. No database schema, user accounts, Auth settings, or RLS policies were changed.

## Official references
- Android authorization: https://developer.android.com/identity/authorization
- AuthorizationClient API: https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationClient
- Gmail OAuth scopes: https://developers.google.com/workspace/gmail/api/auth/scopes
- OAuth verification: https://developers.google.com/identity/protocols/oauth2/production-readiness/restricted-scope-verification
