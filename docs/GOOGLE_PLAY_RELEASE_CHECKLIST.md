# ATLAS AI — Google Play release checklist

**Current release state: NOT READY for production submission.**

## 1. Android build requirements
- [x] Package ID is `com.hooshmasnooi.app`.
- [x] Adaptive launcher and monochrome icons are present.
- [x] Cleartext network traffic is disabled.
- [x] Main-branch GitHub Actions workflow runs Android unit tests and assembles a debug package on pushes.
- [x] Target API is Android 16 / API 36.
- [ ] Run and review the latest CI result and resolve any failures.
- [ ] Test on physical Android devices across supported screen sizes and Android versions.
- [ ] Complete release build configuration, code shrinking review, and crash-free startup checks.
- [ ] Increment versionCode for the first public release and every later update.

Re-check the current Google Play target API policy immediately before submission:
https://support.google.com/googleplay/android-developer/answer/11926878

## 2. Signing and Android App Bundle
- [ ] User supplies or creates a dedicated upload keystore and securely backs it up outside Git.
- [x] Manual signed-AAB validation workflow reads signing material only from protected GitHub Actions secrets and does not publish.
- [ ] Configure repository secrets: `ATLAS_UPLOAD_KEYSTORE_BASE64`, `ATLAS_UPLOAD_STORE_PASSWORD`, `ATLAS_UPLOAD_KEY_ALIAS`, and `ATLAS_UPLOAD_KEY_PASSWORD`.
- [ ] Run signed-AAB validation after the signing secrets are configured.
- [ ] Generate and verify a signed Android App Bundle (.aab).
- [ ] Enroll in Play App Signing and register the upload certificate.
- [ ] Add the release/upload certificate SHA-1 to the Google OAuth Android client if Gmail remains in the release.
- [ ] Verify the uploaded bundle and Play-generated APKs in Play Console.

Google Play App Bundle and Play App Signing references:
https://developer.android.com/guide/app-bundle/faq
https://support.google.com/googleplay/android-developer/answer/9842756

## 3. Backend, AI, and data security
- [x] Android and browser clients are wired to the deployed `atlas-chat` Edge Function.
- [x] Provider credentials are read server-side, not embedded in client source.
- [ ] Confirm that Supabase project `wwlyvojtsrklcpqsloqz` is the intended ATLAS-only backend. Its public schema currently contains 59 exchange-office, wallet, payment, and user-related tables; reported row counts were zero at inspection. Do not add or modify database resources until the project/data boundary is confirmed.
- [ ] Confirm server-side provider Secret names and model configuration, then verify a real successful model response.
- [ ] Select and implement user authentication; the current Edge Function accepts requests without a user JWT.
- [ ] Replace ephemeral IP throttling with durable per-user quotas, atomic usage accounting, abuse protection, and monitoring.
- [ ] Decide whether cloud chat history is required; if so, implement it only in a confirmed isolated ATLAS data boundary.
- [ ] Complete data export/deletion and account lifecycle behavior if accounts or cloud sync are enabled.
- [ ] Complete privacy, permission, accessibility, and data-flow review.

## 4. Integrations and functionality
- [ ] Verify Gmail read-only OAuth and inbox retrieval on physical Android devices.
- [ ] Complete Google OAuth verification and restricted-scope review if public Gmail access remains enabled.
- [ ] Implement file/document, image, voice, web research, and coding tools before advertising them as live capabilities.
- [ ] Ensure every inactive tool is labelled accurately in the app and store listing.

## 5. Privacy and policy
- [ ] Review and finalize Persian/Dari and English privacy policy.
- [ ] Publish the privacy policy at a public HTTPS URL.
- [ ] Provide a working support contact and developer identity.
- [ ] Complete the Play Console Data safety form based on the actual release build and data flows.
- [ ] Complete app access instructions, content rating, target audience, ads declaration, and any required policy declarations.

## 6. Store listing
- [ ] Finalize app title and short description.
- [ ] Write full store description in Persian/Dari and English.
- [ ] Prepare phone screenshots from the real working release build.
- [ ] Prepare the 512×512 app icon and 1024×500 feature graphic.
- [ ] Provide support website/email and privacy policy URL.
- [ ] Do not advertise features that remain inactive.

## 7. Testing tracks and production
- [ ] Upload signed AAB to Internal testing.
- [ ] Install from Google Play testing track and verify chat, Gmail (if included), permissions, offline behavior, and upgrades.
- [ ] Run Closed testing and collect documented feedback.
- [ ] If the developer account is a personal account created after 13 November 2023, confirm whether the 12-testers/14-days closed-testing requirement applies.
- [ ] Apply for production access if required, then request production rollout.
- [ ] Monitor crashes, ANRs, policy messages, and user feedback after launch.

Official testing requirement:
https://support.google.com/googleplay/android-developer/answer/14151465

## 8. Release blockers
1. Supabase database identity/data boundary requires confirmation before adding authentication, quotas, or persistent user data.
2. Provider Secret configuration and a successful live model response are not verified.
3. The current public chat endpoint does not require user JWT and uses non-durable in-memory limits.
4. Gmail OAuth has not been verified on a physical device.
5. Local chat storage is SharedPreferences, not a transactional database.
6. File, image, voice, web, and other advanced tools are not live.
7. Physical-device, accessibility, localization, and security review remain incomplete.
8. Privacy policy, support contact, and store listing need finalization.
9. Release signing key is not configured; the user's release key will be provided later.
10. Signed AAB and Google Play track validation remain outstanding.

A successful debug build is not proof of production readiness. Production is permitted only after the blockers above have evidence and the final signed AAB has been validated through Google Play.
