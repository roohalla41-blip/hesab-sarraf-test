# ATLAS AI — Security Decision Records

## ADR-001: Protecting local conversation history

**Status:** Proposed; not implemented.

### Context
The current Android client stores conversation history in `atlas_chat_history.json` under private internal app storage. Existing installations may already contain user conversations. Encrypting the file without a migration plan could make those conversations unreadable after an interrupted write, app update, or Keystore key loss.

### Decision
Do not change the current storage format until a versioned migration is implemented and tested. The intended target is authenticated encryption with AES-GCM and a randomly generated data-encryption key protected by Android Keystore.

### Required migration behavior
1. Detect and parse the existing plaintext JSON format without modifying it.
2. Generate or retrieve a versioned Keystore-backed key.
3. Encrypt the complete validated history into a new temporary versioned file, using a fresh nonce and authenticated format metadata.
4. Flush the temporary file and verify that it decrypts and parses to the expected conversation/message counts.
5. Atomically promote the encrypted file where the filesystem supports it; retain the original until verification and promotion succeed.
6. On restart, safely resume or roll back an interrupted migration without silently discarding either copy.
7. After successful migration, remove plaintext and any `.corrupt` recovery copy according to a documented retention rule.
8. If the Keystore key is permanently unavailable, show a clear recovery error. Do not silently replace the key and overwrite encrypted history.

### Required tests before release
- Existing empty and populated legacy files.
- Large history at configured retention limits.
- Process interruption at every migration stage.
- Truncated, modified, and invalid ciphertext.
- Missing, invalidated, or unavailable Keystore key.
- Low-storage and file-permission failures.
- Upgrade and downgrade behavior.
- Confirm individual delete and clear-all delete remove all relevant current-format files and recovery copies.

## ADR-002: Account identity and authenticated AI requests

**Status:** Proposed; not implemented.

### Context
The Android scaffold currently has no account authentication. A client-side publishable key or shared app token cannot establish a unique user identity. The deployed Supabase `ai-assistant` function is a legacy حساب صراف function and is not an approved ATLAS endpoint.

### Decision
When account features are authorized, use Supabase Auth user sessions and a separate ATLAS backend endpoint. The backend must validate user access tokens and enforce per-user authorization. The Android app must never contain provider keys, service-role keys, or other privileged shared secrets.

### Required controls
- Verify user access tokens server-side; do not trust user IDs supplied in request bodies.
- Apply authorization checks to every account-owned object.
- Keep provider keys in server-side secrets only.
- Enforce durable, atomic per-user quotas and abuse limits before public rollout.
- Bound request size, execution time, and upstream response size.
- Keep prompts and transcripts out of logs by default; log only minimal operational metadata.
- Make cloud history opt-in and document retention, export, and deletion.
- Clear local session material on sign-out.
- Keep local history deletion, cloud account deletion, and provider-side retention as separate documented operations.

### Explicit exclusions
- Do not repurpose or modify the live legacy `ai-assistant` function as part of this proposal.
- Do not deploy an endpoint or change Supabase schema, Auth, RLS, secrets, or production settings under this ADR alone.
- Do not enable the Android gateway until a separate ATLAS endpoint has passed review and deployment has been explicitly authorized.

## ADR-003: Privacy and deletion claims

**Status:** Adopted for documentation; implementation remains partial.

- The app currently offers individual and clear-all deletion on the History screen.
- There is no in-app export, account deletion, cloud sync, or app-level encryption.
- The history repository preserves a damaged JSON file as a `.corrupt` copy. Future deletion and migration work must account for that copy.
- Product copy must not claim capabilities that are only planned.
