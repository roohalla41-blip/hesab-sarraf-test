# ATLAS AI — Privacy and Safety Baseline

## Privacy principles
1. Collect only data required to provide the feature the user requested.
2. Keep local history separate from optional cloud synchronization.
3. Explain what is stored, where it is stored, and how long it is retained before enabling cloud features.
4. Provide controls for history deletion, account deletion, data export, and privacy preferences.
5. Make optional analytics and model-improvement use opt-in rather than silently enabled.
6. Do not place secrets, credentials, or sensitive user content in application logs.
7. Treat uploaded files as private user data; validate and restrict access.

## Local chat history retention
- Chat history is stored locally in the app's private SharedPreferences file.
- Each conversation retains at most 300 messages. When the limit is exceeded, the oldest messages are automatically removed and the newest 300 are retained.
- This limit is a retention policy, not a cloud backup or cross-device synchronization feature.
- The app retains one previous local conversation snapshot to recover from a malformed current snapshot. This is a limited local recovery copy, not a guaranteed backup; clearing app data or uninstalling the app can remove both copies.

## Account and session principles
- Authentication is not implemented in the current scaffold.
- Session tokens, when introduced, must use platform-appropriate secure storage.
- Never store passwords in plain text.
- Sign-out must clear locally held session material.
- The server must enforce authorization for every account-owned resource.

## AI safety principles
- Clearly distinguish generated content from verified facts.
- Provide a way to report harmful or inaccurate responses.
- Apply server-side abuse prevention and rate limits.
- Do not claim that web research, voice, image, file, or code tools are available until the relevant service is implemented and tested.
- Give users a clear indication when a request fails or is cancelled.

## Release gate
Before public release, publish a plain-language privacy notice and terms, complete security review, test account/data deletion, validate permission prompts, and verify all claims against actual implemented behavior.
