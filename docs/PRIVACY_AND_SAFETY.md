# ATLAS AI — Privacy and Safety Baseline

## Privacy principles
1. Collect only data required to provide the feature the user requested.
2. Keep local history separate from optional cloud synchronization.
3. Explain what is stored, where it is stored, and how long it is retained before enabling cloud features.
4. Provide controls for history deletion, account deletion, data export, and privacy preferences.
5. Make optional analytics and model-improvement use opt-in rather than silently enabled.
6. Do not place secrets, credentials, or sensitive user content in application logs.
7. Treat uploaded files as private user data; validate and restrict access.

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
