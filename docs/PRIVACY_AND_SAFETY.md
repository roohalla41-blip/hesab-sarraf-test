# ATLAS AI — Privacy and Safety Baseline

## Privacy principles
1. Collect only data required to provide the feature the user requested.
2. Keep local history separate from optional cloud synchronization.
3. Explain what is stored, where it is stored, and how long it is retained before enabling cloud features.
4. Provide controls for history deletion, account deletion, data export, and privacy preferences before claiming those controls exist.
5. Make optional analytics and model-improvement use opt-in rather than silently enabled.
6. Do not place secrets, credentials, or sensitive user content in application logs.
7. Treat uploaded files as private user data; validate and restrict access.

## Current local history behavior
- Conversation history is stored as JSON in a file under Android private internal app storage.
- The current implementation is device-local and does not sync to Supabase or another cloud service.
- Android app sandboxing restricts ordinary access by other apps; this implementation does not add application-level encryption.
- Users should not put passwords, API keys, or other secrets into chat messages.
- Retention is bounded to the newest 40 conversations and 300 messages per conversation.
- Users can delete one conversation from History after confirmation.
- Users can clear all conversations from History or the Privacy and Security screen after a separate confirmation. This operation also attempts to remove the damaged-history recovery copy.
- The Privacy and Security screen displays the local conversation count and explicitly states that application-level encryption, online sign-in, and AI service connection are not yet active.
- There is no in-app export control.
- Clearing app data or uninstalling the app also removes the local history file.
- If the main history JSON is malformed, the repository preserves one `.corrupt` recovery copy in private app storage before returning an empty history. Individual conversation deletion does not currently inspect or selectively edit this recovery copy. Clear-all removes it; if removal fails, the operation reports an error.
- Writes now flush a same-directory temporary file and attempt an atomic replacement, falling back to a same-directory replacement operation rather than truncating the existing file directly. Filesystem guarantees still vary, and this behavior has not yet been device-tested.
- Automated Robolectric/JUnit tests are configured for JSON persistence, including retention, recovery, deletion, duplicate IDs, and concurrent writes. They have been authored but have not yet been executed; a passing test run remains a release gate.

## Local encryption decision
- Do not silently encrypt or rename the existing history file in-place.
- Before enabling encryption, implement and test a versioned migration that preserves existing plaintext history until the encrypted copy has been fully written and verified.
- Use Android Keystore to protect a randomly generated AES-GCM key; never hard-code or export that key.
- Include authenticated metadata (format version and nonce) and reject tampered ciphertext safely.
- Define recovery behavior for missing/invalid Keystore keys, app restore, device transfer, and interrupted migration.
- Keep a recoverable backup until verification succeeds; then apply a documented cleanup policy.
- This is a design requirement, not an implemented feature.

## Account and session principles
- Authentication is not implemented in the current scaffold.
- Proposed future sign-in uses Supabase Auth user sessions and short-lived access tokens, with refresh handled by the supported client SDK.
- Never store passwords in plain text or embed service-role/provider secrets in the Android app.
- Store session material only using platform-appropriate secure storage and clear it on sign-out.
- The backend must validate the user's access token and enforce ownership for every account-owned resource.
- Account deletion must separately define deletion of cloud records, local history, and provider-side retention; do not imply one automatically deletes the others.

## AI service safety
- The Android app must call an authenticated ATLAS-specific backend, never a provider endpoint directly.
- Provider credentials belong only in the server Secret Manager.
- Require server-side authentication, authorization, input limits, abuse prevention, and durable quotas before public rollout.
- In-memory counters are not sufficient for globally consistent limits in a multi-instance serverless deployment.
- Do not retain prompts or transcripts by default. Any optional server-side history or diagnostics must have a clear purpose, retention period, access policy, and user-facing disclosure.
- Clearly distinguish generated content from verified facts.
- Do not claim that web research, voice, image, file, or code tools are available until the relevant service is implemented and tested.
- Give users a clear indication when a request fails or is cancelled.

## Release gate
Before public release, publish a plain-language privacy notice and terms, complete a security review, test local and account/data deletion, validate permission prompts, verify all privacy claims against actual behavior, and test migration/recovery paths. No production endpoint should be enabled until its authentication, quota, and privacy controls are independently reviewed.
