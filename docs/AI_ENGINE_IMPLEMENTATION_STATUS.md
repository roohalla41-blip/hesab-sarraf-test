# ATLAS AI — AI Engine Implementation Status

This is a repository-only implementation draft. It does not deploy or configure Supabase or an external provider.

## Implemented in this repository
- Provider-neutral Android gateway boundary with explicit failure states.
- Configurable Android HTTP adapter with HTTPS-only endpoint validation, bounded response size, timeouts, and safe error mapping.
- Adapter keeps provider credentials out of Android and supports an optional app-session token.
- JSON-backed local conversation repository in Android private internal storage.
- Chat screen creates conversations, persists user messages, restores the latest conversation on launch, and opens saved conversations from History.
- History screen supports per-conversation deletion and clear-all deletion with confirmation dialogs.
- Clear-all attempts to remove the damaged-history recovery copy and reports failure if that copy cannot be deleted.
- Local history writes flush a same-directory temporary file and use atomic replacement when supported, with a replacement fallback that avoids directly truncating the previous file.
- Appearance preview and language selection preferences are persisted locally on the device.
- Local history read, write, and delete failures surface a user-visible error dialog.
- Local history is bounded to 40 conversations and 300 messages per conversation.
- Server-side chat function draft reads provider credentials only from server environment variables.
- Validation for supported modes, message roles, per-message size, total context size, and final user turn.
- Request correlation IDs, bounded upstream timeout, HTTPS-only provider endpoint configuration, and stable user-safe error codes.
- Optional exact-origin allowlist through ATLAS_ALLOWED_ORIGINS; native requests without an Origin header are supported.
- Provider response parsing with optional token usage metadata.
- Supabase function source configuration keeps JWT verification enabled; it has not been deployed.

## Not implemented or not yet verified
- The Android adapter is not wired into MainActivity or a ViewModel and is not enabled in the app.
- The currently deployed `ai-assistant` function still contains the legacy حساب صراف system prompt and reads `GROQ_API_KEY` / `GROQ_MODEL`; it is not an ATLAS production endpoint.
- No confirmed successful live model request from the Android app.
- Local history is not encrypted at the application layer and has no export control.
- A malformed-history recovery copy may remain after individual conversation deletion; clear-all removes it if filesystem deletion succeeds.
- Atomic file replacement behavior has not been tested on a physical Android device or under forced process interruption.
- Theme selection currently persists as a preview preference; full dynamic color-scheme switching is not complete.
- Language selection currently persists as a preference; full translations and device-language auto-detection are not complete.
- No streaming/SSE or Android cancellation propagation.
- No atomic per-account quota, durable per-IP rate limiting, abuse controls, moderation pipeline, or usage ledger.
- No account authentication flow or cloud conversation synchronization.
- No file, image, audio, web, or coding tool execution.
- No production monitoring or automated integration tests.

## Security and rollout gates
1. Keep the legacy `ai-assistant` endpoint unchanged until a separate ATLAS endpoint and migration plan are approved.
2. Review authentication claims and ownership rules before enabling requests. Do not rely on an app-embedded secret or a publishable key as user authentication.
3. Add an atomic quota/rate-limit store before public rollout; in-memory counters are not sufficient in serverless environments.
4. Configure provider credentials only in the server Secret Manager. Never commit values or place them in Android.
5. Set exact trusted web origins before browser access.
6. Add local-storage tests, adapter tests, and integration tests, then explicitly authorize deployment. This change does not deploy anything.

## Current status
The Android gateway remains disabled and disconnected from the legacy live endpoint. Local chat history is connected to the UI on a work branch, including per-conversation deletion, clear-all history, safer file replacement, visible persistence errors, and locally persisted appearance/language preferences. The production AI engine is not complete. Review and test all source before deployment.
