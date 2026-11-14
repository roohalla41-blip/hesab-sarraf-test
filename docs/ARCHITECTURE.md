# ATLAS AI — Application Architecture

## Product direction
ATLAS AI is designed as a multilingual, globally shareable AI assistant. The initial experience is Persian-first, with a localization architecture that can expand to Dari, English, and additional languages.

## Architecture boundaries
- **app/core** — app-wide configuration and cross-cutting foundations.
- **domain/model** — provider-independent business models.
- **domain/repository** — contracts for history and assistant operations.
- **domain/usecase** — user actions and orchestration.
- **data/remote** — the server API boundary. Provider keys remain server-side.
- **data/local** — future on-device conversation storage.
- **feature/** — independent product areas and their UI state.

## Planned product areas
1. Chat: streaming responses, regenerate, edit/resend, stop generation, copy, share, feedback.
2. History: search, rename, pin, archive, delete, and restore where supported.
3. Explore: curated assistant modes and guided prompts.
4. Tools: document analysis, image understanding, web research, and coding tools, enabled only when backed by secure services.
5. Voice: speech input, text-to-speech, interruption, and hands-free conversation.
6. Account and plans: sign-in, usage limits, subscription status, and billing integrations.
7. Settings: language, appearance, privacy, data export, and accessibility.
8. Safety: abuse prevention, rate limits, reporting, and clear capability boundaries.

## AI service security
- No provider secret or privileged backend key in the Android app.
- The app talks only to an authenticated application backend.
- The backend owns provider selection, quotas, model routing, moderation, and usage accounting.
- Secrets are configured in the server's secret manager, never committed to Git.
- Requests have bounded input sizes and explicit error states.

## Data strategy
- Local conversation storage is a separate concern from the AI provider.
- Cloud sync and account storage are postponed until the user authorizes the selected backend.
- No production database, authentication, or server function is changed as part of this scaffold.

## Current implementation status
The current Android screen is a UI foundation. The AI gateway and history repository are contracts only; they are not connected to a live service or database. Voice, image, file, web, coding, account, and subscription capabilities are planned and must not be presented as active until implemented and tested.
