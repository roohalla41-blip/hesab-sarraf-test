# ATLAS AI — Application Architecture

## Product areas and actual status
1. **Chat:** Compose UI is present. Messages are stored locally; no AI model is connected.
2. **History:** AES-256-GCM encrypted local conversation history, verified legacy migration, bounded retention, restore, single delete, and clear-all delete are wired to Android UI.
3. **Explore:** assistant modes are presentation-only.
4. **Tools:** file, image, web research, and coding tools are not implemented.
5. **Voice:** speech input/output is not implemented.
6. **Account and plans:** Google sign-in, guest identity, server quotas, subscriptions, and billing are not implemented.
7. **Settings:** light/dark themes are implemented and persisted locally; language choice is stored as a preference only, while full localization and system-language switching are not complete.
8. **Privacy:** local-history controls and Android Keystore-backed encryption are implemented; data export is not implemented.

## Architecture boundaries
- **app/core:** app-wide configuration.
- **domain/model:** provider-independent models.
- **domain/repository:** history and account contracts.
- **data/local:** encrypted JSON history in Android private internal storage, with Android Keystore key management.
- **data/remote:** reserved for an authenticated ATLAS API; currently not connected.
- **feature:** UI areas and state.

## AI service security requirements
- Android must call only an authenticated ATLAS-specific backend.
- Provider secrets and privileged database credentials must remain in server Secret Manager.
- Backend must verify user identity, enforce authorization, input limits, abuse controls, durable atomic quotas, and usage accounting.
- In-memory counters alone are insufficient for globally consistent limits.
- Do not connect the existing public/unaudited chat endpoint to the app.
- No Supabase schema, auth provider, secrets, users, or Edge Functions are changed by this branch work.

## Local data protection
- Current history is a bounded JSON payload encrypted with AES-256-GCM and stored in Android private internal storage. Its 256-bit non-exportable key is managed by Android Keystore.
- It keeps up to 40 conversations and 300 messages per conversation. When the message cap is exceeded, the oldest message IDs are trimmed automatically.
- Conversation metadata and message append are persisted in one mutex-protected write; retained messages are ordered by monotonically increasing message ID so concurrent appends preserve the newest 300.
- Legacy plaintext JSON is migrated to a separate encrypted file and removed only after decrypt-and-compare verification. Malformed legacy data and any pre-existing legacy recovery copy are preserved in encrypted recovery files.
- Clear-all writes an empty encrypted history before removing encrypted recovery copies.
- Local deletion, cloud account deletion, and provider retention are separate lifecycle concerns.
- The encrypted file has a format marker/version and nonce. Missing keys or authentication failures fail closed; history is not silently reset. Android Keystore and migration still require physical-device validation.

## Release blockers
- Google Sign-In for app account authentication.
- Secure server authentication and durable guest quota.
- Real AI gateway integration and provider failover.
- Execute the new automated repository tests and complete Android build validation.
- Device testing, privacy review, accessibility, complete translations, signed AAB, Play Console policy declarations, and store assets.


## Local conversation history security

Conversation history is stored locally as JSON payload encrypted with AES-256-GCM. The 256-bit key is generated and retained in Android Keystore and is not exported by the application. Legacy plaintext JSON is migrated to a separate encrypted file; the old file is removed only after decrypt-and-compare verification. Missing keys or authentication failures stop history loading rather than silently creating an empty history. Android Keystore and file-migration behavior still require physical-device testing.
