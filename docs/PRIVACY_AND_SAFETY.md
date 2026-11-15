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
- Conversation history is serialized as JSON, then encrypted into a binary file under Android private internal app storage.
- The current implementation is device-local and does not sync to Supabase or another cloud service.
- New history writes are encrypted with AES-256-GCM. The non-exportable 256-bit key is generated and stored by Android Keystore; the key is not embedded in the APK or written beside the history file.
- Users should not put passwords, API keys, or other secrets into chat messages.
- Retention is bounded to the newest 40 conversations and 300 messages per conversation.
- Users can delete one conversation from History after confirmation.
- Users can clear all conversations from History or the Privacy and Security screen after a separate confirmation. This operation also attempts to remove the damaged-history recovery copy.
- The Privacy and Security screen displays the local conversation count and explains that local history is encrypted, while online sign-in and AI service connection are not yet active.
- There is no in-app export control.
- Clearing app data or uninstalling the app also removes the local history file.
- Legacy JSON migration verifies the encrypted copy by decrypting it and comparing the original bytes before deleting the plaintext file. Malformed legacy JSON is preserved as an encrypted recovery file before an empty encrypted history is initialized. A pre-existing legacy `.corrupt` recovery file is also encrypted before its plaintext copy is removed. Clear-all removes encrypted recovery files after writing an empty encrypted history.
- Encrypted writes use a same-directory temporary file, flush it, verify decryption before replacement, then atomically replace the target where supported. Filesystem guarantees still vary, and this behavior has not yet been device-tested.
- Automated Robolectric/JUnit tests cover encrypted persistence through a deterministic test cipher, legacy migration, migration failure preservation, encrypted recovery, retention, deletion, duplicate IDs, and concurrent writes. They have been authored but have not yet been executed; a passing test run remains a release gate. The production Android Keystore implementation still requires on-device validation.

## Local encryption decision
- Do not silently encrypt or rename the existing history file in-place. The implemented migration writes a separate encrypted file, verifies it, and only then removes the legacy plaintext file.
- Android Keystore now protects a randomly generated AES-256-GCM key; the key is non-exportable and is not hard-coded.
- The encrypted file includes a format marker/version and nonce; AES-GCM authentication rejects modified ciphertext. Missing keys fail closed rather than generating a replacement key.
- Recovery from missing/invalid Keystore keys, app restore, and device transfer remains a release-planning item. Interrupted migration is retryable because plaintext is retained until encrypted verification succeeds.
- The migration retains damaged legacy data as an encrypted recovery file; clear-all removes that recovery file after committing an empty encrypted history.
- Recovery from a missing, invalidated, or hardware-reset Keystore key remains a documented limitation; the app must not silently reset encrypted history. This implementation still requires physical-device testing.

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
