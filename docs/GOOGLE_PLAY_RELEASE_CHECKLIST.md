# ATLAS AI — Google Play release checklist

**Current release state:** Not ready for production submission.

## 1. Android build requirements
- [x] Package ID is set to com.hooshmasnooi.app.
- [x] Adaptive launcher icon and monochrome icon are present.
- [x] Cleartext network traffic is disabled.
- [x] CI validates Android tests and package assembly.
- [x] Target API updated to Android 16 / API 36 for new-app submission policy.
- [ ] Re-run the full build after the API 36 toolchain upgrade.
- [ ] Test on physical Android devices across supported screen sizes and Android versions.
- [ ] Complete release build configuration, code shrinking review, and crash-free startup checks.
- [ ] Increment versionCode for the first release and every subsequent update.

Google Play requires new apps and app updates submitted from 31 August 2026 to target Android 16 / API 36 or higher. Re-check the official policy immediately before submission:
https://support.google.com/googleplay/android-developer/answer/11926878

## 2. Signing and Android App Bundle
- [ ] Create and securely back up a dedicated upload keystore outside the Git repository.
- [ ] Configure release signing from protected CI secrets; never commit the keystore or passwords.
- [ ] Generate a signed Android App Bundle (.aab), not a debug APK.
- [ ] Enroll in Play App Signing and register the upload certificate.
- [ ] Add the correct release/upload certificate SHA-1 to the Google OAuth Android client if Gmail remains in the release.
- [ ] Verify the uploaded bundle and Play-generated APKs through Play Console.

New Play apps use Android App Bundles and Play App Signing:
https://developer.android.com/guide/app-bundle/faq
https://support.google.com/googleplay/android-developer/answer/9842756

## 3. Developer account and package identity
- [ ] Complete Google Play developer identity verification.
- [ ] Register the package name com.hooshmasnooi.app in Play Console if required for the developer account.
- [ ] Confirm the developer account type and whether the new-personal-account closed-testing rule applies.

Google's package registration policy states that all Play packages must be registered from 30 September 2026:
https://support.google.com/googleplay/android-developer/answer/16984799

## 4. App functionality and security
- [ ] Connect the Android chat UI to the authenticated ATLAS Edge Function.
- [ ] Confirm the independent ATLAS Supabase project and user sign-in method; do not use the protected Sarraf database.
- [ ] Configure provider secrets only in the server Secret Manager and test Z.AI/Cloudflare fallback with cost controls.
- [ ] Add atomic quotas, abuse controls, monitoring, and provider-failure tests.
- [ ] Replace the current SharedPreferences conversation store with a transactional local database.
- [ ] Verify Gmail OAuth and read-only inbox retrieval on physical devices.
- [ ] Complete account/data export and deletion behavior if accounts or cloud sync are enabled.
- [ ] Complete privacy, permission, accessibility, and data-flow review.

## 5. Privacy and policy
- [ ] Review and finalize the Persian and English privacy policy drafts.
- [ ] Publish the privacy policy at a public HTTPS URL.
- [ ] Provide a working support contact and developer identity.
- [ ] Complete the Play Console Data safety form based on the actual release build and data flows.
- [ ] Complete app access instructions, content rating, target audience, ads declaration, and any required policy declarations.
- [ ] Complete Google OAuth verification and restricted-scope review if Gmail access remains enabled for public users.

## 6. Store listing
- [ ] Finalize app title and short description.
- [ ] Write the full store description in Persian and English.
- [ ] Prepare phone screenshots for the real, working release build.
- [ ] Prepare the 512×512 app icon and 1024×500 feature graphic.
- [ ] Provide support website/email and privacy policy URL.
- [ ] Ensure screenshots and listing claims do not advertise features that are still inactive.

## 7. Testing tracks and production
- [ ] Upload the signed AAB to Internal testing first.
- [ ] Install from Google Play testing track and verify Gmail, chat, permissions, offline behavior, and upgrades.
- [ ] Run Closed testing and collect documented feedback.
- [ ] If the developer account is a personal account created after 13 November 2023, keep at least 12 testers opted in continuously for 14 days before applying for production access.
- [ ] Apply for production access if required, answer Play Console testing questions, then request production rollout.
- [ ] Monitor crashes, ANRs, policy messages, and user feedback after launch.

Official personal-account testing requirement:
https://support.google.com/googleplay/android-developer/answer/14151465

## 8. Current blockers
1. General AI is not live; the provider fallback function and Android adapter are source-only and not connected to a real authenticated project.
2. Gmail has not been tested on a physical device.
3. Local conversation storage is not yet on a transactional database.
4. Release signing and AAB pipeline are not configured.
5. Privacy policy and store listing are drafts, not published.
6. API 36 build upgrade must pass CI and device testing.

A successful debug build is not proof of production readiness. Production is permitted only after the blockers above are resolved and the final AAB has been tested through Play Console.
