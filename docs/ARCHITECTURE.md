# ATLAS AI — Application Architecture

## Product direction
ATLAS AI is a multilingual, globally shareable AI assistant. The initial experience is Persian-first and is designed to expand to Dari, English, and additional languages.

## Architecture boundaries
- **app/core** — app-wide configuration and cross-cutting foundations.
- **domain/model** — provider-independent business models.
- **domain/repository** — contracts for history and assistant operations.
- **domain/usecase** — user actions and orchestration.
- **data/local** — JSON-backed conversation history in Android private internal storage.
- **data/remote** — the server API boundary; provider keys remain server-side.
- **feature/** — independent product areas and their UI state.

## Product areas and actual status
1. Chat: message composition and local conversation continuity are scaffolded; no model response is currently connected. Streaming, regenerate, edit/resend, stop, copy, share, and feedback remain future work.
2. History: local conversation list, restore, bounded retention, individual deletion, and clear-all deletion are connected. Export is not implemented.
3. Explore: curated assistant modes and guided prompts are currently presentation-only.
4. Tools: document analysis, image understanding, web research, and coding tools are not implemented.
5. Voice: speech input, text-to-speech, interruption, and hands-free conversation are not implemented.
6. Account and plans: sign-in, usage limits, subscriptions, and billing are not implemented.
7. Settings: language and appearance preferences persist locally; full translations, device-language detection, and dynamic theme switching are not complete.
8. Privacy: an informational page is present; local history deletion controls are on the History page. Encryption and export are not implemented.

## AI service security
- No provider secret or privileged backend key may be embedded in the Android app.
- The app must talk only to an authenticated ATLAS-specific application backend.
- The backend owns provider selection, quotas, model routing, moderation, and usage accounting.
- Secrets are configured in the server secret manager, never committed to Git.
- Requests require bounded input sizes, timeouts, and explicit error states.
- The Android gateway adapter is present but is not enabled or connected to the UI.
- The currently deployed Supabase `ai-assistant` function is a legacy حساب صراف endpoint and must not be used as the ATLAS backend.

## Proposed identity and API boundary (not implemented)
- Use Supabase Auth user sessions; the app must not treat an anonymous client, publishable key, or app-embedded shared token as proof of user identity.
- Require a valid user access token at the ATLAS backend and verify it server-side.
- Enforce ownership and authorization for every user-specific resource.
- Keep provider credentials and privileged database credentials server-side only.
- Add durable, atomic quota and rate-limit enforcement before public exposure; in-memory counters alone are insufficient.
- Keep chat content out of server logs by default. Record only minimal operational metadata such as request ID, status, latency, and usage totals where justified.
- Cloud conversation sync remains opt-in and requires a separately reviewed data model, retention policy, and deletion path.

## Local data protection
- Current history is a bounded JSON file in Android private internal storage, without application-level encryption.
- Do not add encryption until a versioned, interruption-safe migration has been designed and tested against existing history files.
- Future encryption should use an AES-GCM data key protected by Android Keystore, with explicit key-loss and recovery behavior.
- Local deletion, cloud account deletion, and provider retention are separate lifecycle concerns.

## Data strategy
- Local conversation storage is separate from the AI provider.
- Cloud sync and account storage are postponed until the selected backend is authorized and reviewed.
- No production database, authentication, or server function is changed by this scaffold.

## Current status
The Android chat UI is connected to the local history repository and restores the latest saved conversation on launch. Individual and clear-all deletion are available from History. The remote AI adapter remains disabled and disconnected from the UI. The privacy screen is informational only. No build or device test has been run in this work stage.
