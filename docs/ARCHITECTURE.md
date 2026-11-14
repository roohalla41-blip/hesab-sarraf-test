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

## Product areas
1. Chat: message composition and local conversation continuity are scaffolded; streaming, regenerate, edit/resend, stop, copy, share, and feedback remain future work.
2. History: local conversation list, restore, and bounded retention are connected; delete/export controls remain future work.
3. Explore: curated assistant modes and guided prompts.
4. Tools: document analysis, image understanding, web research, and coding tools.
5. Voice: speech input, text-to-speech, interruption, and hands-free conversation.
6. Account and plans: sign-in, usage limits, subscriptions, and billing.
7. Settings: language, appearance, privacy, data export, and accessibility.
8. Safety: abuse prevention, rate limits, reporting, and clear capability boundaries.

## AI service security
- No provider secret or privileged backend key in the Android app.
- The app talks only to an authenticated application backend.
- The backend owns provider selection, quotas, model routing, moderation, and usage accounting.
- Secrets are configured in the server secret manager, never committed to Git.
- Requests have bounded input sizes and explicit error states.
- The Android gateway is not enabled against the current live legacy function.

## Data strategy
- Local conversation storage is separate from the AI provider.
- Current history is stored in a bounded JSON file under Android private internal storage; this version does not add application-level encryption.
- Cloud sync and account storage are postponed until the selected backend is authorized.
- No production database, authentication, or server function is changed by this scaffold.

## Current status
The Android chat UI is connected to the local history repository and restores the latest saved conversation on launch. The remote AI adapter is present but remains disabled and disconnected from the UI. Voice, image, file, web, coding, account, subscription, and cloud-sync capabilities must not be shown as active until implemented and tested. No build or device test has been run in this work stage.
