# ATLAS AI — Privacy and Safety Baseline

## Privacy principles
1. Collect only data required to provide the feature the user requested.
2. Keep local history separate from optional cloud synchronization.
3. Explain what is stored, where it is stored, and how long it is retained before enabling cloud features.
4. Provide controls for history deletion, account deletion, data export, and privacy preferences.
5. Make optional analytics and model-improvement use opt-in rather than silently enabled.
6. Do not place secrets, credentials, or sensitive user content in application logs.
7. Treat uploaded files as private user data; validate and restrict access.

## Current local history behavior
- Conversation history is stored in a JSON file under Android's private internal app storage.
- The current implementation is device-local and does not sync to Supabase or another cloud service.
- Android app sandboxing restricts ordinary access by other apps; this implementation does not add application-level encryption.
- Users should not put passwords, API keys, or other secrets into chat messages.
- The app currently limits retention to the newest 40 conversations and 300 messages per conversation.
- In-app deletion and export controls are not implemented yet. Clearing app data or uninstalling the app removes this local file.

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
