# ATLAS AI — Application Architecture

## Product areas and actual status
1. **Chat:** Compose UI is present. Messages are stored locally; no AI model is connected.
2. **History:** JSON-backed local conversation list, restore, bounded retention, single delete, and clear-all delete are wired to Android UI.
3. **Explore:** assistant modes are presentation-only.
4. **Tools:** file, image, web research, and coding tools are not implemented.
5. **Voice:** speech input/output is not implemented.
6. **Account and plans:** Google sign-in, guest identity, server quotas, subscriptions, and billing are not implemented.
7. **Settings:** local preview preferences exist; full localization and system-language switching are not complete.
8. **Privacy:** local-history controls are present; application-level encryption and data export are not implemented.

## Architecture boundaries
- **app/core:** app-wide configuration.
- **domain/model:** provider-independent models.
- **domain/repository:** history and account contracts.
- **data/local:** JSON history in Android private internal storage.
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
- Current history is a bounded JSON file in Android private internal storage, without application-level encryption.
- It keeps up to 40 conversations and 300 messages per conversation.
- If the main JSON is malformed, one recovery copy is preserved in private app storage.
- Clear-all writes an empty history before removing the recovery copy.
- Local deletion, cloud account deletion, and provider retention are separate lifecycle concerns.
- Application-level encryption needs a versioned, interruption-safe migration using an Android Keystore-protected AES-GCM key; this is not implemented.

## Release blockers
- Google Sign-In for app account authentication.
- Secure server authentication and durable guest quota.
- Real AI gateway integration and provider failover.
- Automated repository tests and Android build validation.
- Device testing, privacy review, accessibility, complete translations, signed AAB, Play Console policy declarations, and store assets.
