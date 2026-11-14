# ATLAS CONNECT — Integration architecture

## Gmail: first practical Android integration

The Android app has a real Gmail read-only integration path in source code, not just a decorative connection card.

### What is implemented
- Google Identity Services `AuthorizationClient` requests the minimum scope currently needed: `https://www.googleapis.com/auth/gmail.readonly`.
- After the user grants consent, ATLAS calls the Gmail REST API and fetches up to 8 messages from the inbox, including sender, subject, date metadata, and snippet.
- The Integrations page displays the fetched messages.
- The main chat recognises direct Gmail-check requests in Persian or English (for example, «ایمیل‌های جدیدم را بررسی کن» / “check my Gmail”) and calls the same Gmail API path. The result is written back as an assistant message in the local conversation.
- The chat history (including Gmail snippets returned in chat) is stored locally in the app's private SharedPreferences. This current storage is app-private but is not encrypted at rest by a dedicated encrypted-storage layer; avoid using a shared/public device for sensitive mailbox testing.
- The access token is held in Activity memory only. It is not written to SharedPreferences, Supabase, or GitHub.
- The current pilot is read-only: it does not send, draft, delete, modify, or archive email.

### Important limitations
- The source changes have not yet been compiled or tested on a physical Android device. Do not describe the integration as verified until an APK is built, installed, authorized, and successfully retrieves real inbox data.
- The Android authorization flow can request a fresh access token again while the user's Google grant remains active. Google access tokens are short-lived (about one hour); server-side offline access would require a separate secure refresh-token architecture.
- The current chat understands a narrow set of explicit Gmail-check phrases. It is not yet a general natural-language AI agent and does not connect arbitrary Gmail content to an LLM for semantic reasoning.
- “Clear connection from this session” removes the in-memory token from this Activity. It does not by itself revoke the Google consent grant; revoke ATLAS AI from the user's Google Account security settings to remove the grant.
- A public release using Gmail user-data scopes may require Google's OAuth verification and additional compliance steps. Development/testing and personal-use exceptions depend on Google's current rules.

## Google Cloud setup required before the first device test
1. Create or select a Google Cloud project.
2. Enable the Gmail API for that project.
3. Configure the OAuth consent screen and add the testing Google account as a test user if the app is in Testing mode.
4. Create an Android OAuth client using the exact package name `com.hooshmasnooi.app` and the SHA-1 fingerprint of the signing certificate used for the APK. Debug and release certificates have different fingerprints.
5. Pushes to `main` trigger the Android debug build workflow. The workflow is configured to preserve its debug signing key between builds, print its SHA-1 fingerprint in the build log, and upload `ATLAS-AI-debug` as a 14-day artifact.
6. Copy the SHA-1 printed by that workflow into the Android OAuth client for package `com.hooshmasnooi.app`. Download and install the matching `ATLAS-AI-debug` artifact.
7. Open **اتصال برنامه‌ها**, tap **اتصال حساب Gmail با Google**, select the test account, and approve read-only Gmail access.
8. In the integration page, confirm that messages load. Then open the main chat and send «ایمیل‌های جدیدم را بررسی کن». Confirm that ATLAS returns the inbox data as an assistant message.

## Other integrations

GitHub, Google Drive, Supabase, and Google Calendar remain directory entries only. Their OAuth flows, per-user credential storage, provider API calls, and tests have not been implemented.

## Security boundaries

- Never embed OAuth client secrets, refresh tokens, service-role keys, or provider API secrets in the Android APK or web JavaScript.
- Do not store user Gmail tokens in a shared global environment secret or a shared database record.
- Any future server-side token storage must be per-user, encrypted, access-controlled, revocable, and tied to a verified app user.
- Keep Gmail scopes minimal and request permissions only when the user invokes the feature.
- Side-effecting actions such as sending or deleting email must require a separate implementation review and explicit user confirmation.
- No Supabase database, authentication, RLS, or Edge Function changes were made for this Gmail chat integration.

## Official references
- Android authorization: https://developer.android.com/identity/authorization
- AuthorizationClient API: https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationClient
- Gmail OAuth scopes: https://developers.google.com/workspace/gmail/api/auth/scopes
- OAuth verification: https://developers.google.com/identity/protocols/oauth2/production-readiness/restricted-scope-verification
