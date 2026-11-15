# Privacy Policy Draft — ATLAS AI

**Status:** Internal draft; not ready for Google Play publication.

**Review date:** November 15, 2026

## 1. App operator
Legal developer name: [Person or company]
Privacy contact: [Support email]
Public privacy-policy URL: [Public URL for this document]

Complete and verify these details before release.

## 2. Current app behavior
This ATLAS AI version is an Android app under development. In the current version:
- Online accounts, cloud sync, payments, and subscriptions are not active.
- The general AI engine is not connected to a model provider; general chat messages are not sent to an AI provider for processing.
- Conversation history is stored in the app's private storage on the same device.
- Gmail is an optional, read-only integration.

## 3. Information processed by the app

### Conversations
Messages you enter and related system responses are stored in the app's private on-device storage so that conversation history remains available. Current storage uses SharedPreferences and does not provide a dedicated encryption-at-rest layer. Do not use this development version on a shared or untrusted device for highly sensitive information.

### Gmail (optional)
If you initiate Gmail connection and approve Google's permission:
- The app requests only the gmail.readonly scope.
- The app reads up to 8 recent inbox messages.
- Displayed information may include sender, subject, date, and a short message preview.
- The Gmail access token is held only in memory for the current app session. It is not stored in SharedPreferences, GitHub, or a cloud database.
- If you request a Gmail check in chat, retrieved email details may be saved in that conversation's local history.
- This version does not send, draft, delete, archive, or modify emails.

You can revoke Google's permission from your Google Account security settings. The in-app disconnect action clears the temporary token for the current session but does not, by itself, revoke the OAuth grant from your Google Account.

## 4. Data transmission
In the current version, conversation text and Gmail content are not sent by the app to an ATLAS server or AI model provider; the general AI engine and cloud sync are not active. OAuth and Gmail API requests communicate directly with Google, and Google's own privacy terms govern Google's processing.

This section must be reviewed and updated before enabling AI APIs, analytics, reporting, or cloud synchronization.

## 5. Data retention and deletion
- Conversations remain on the device while they are stored by the app.
- You can delete individual conversations from the History section.
- Clearing app data in Android settings or uninstalling the app removes its local app data.
- Cloud synchronization and account backups are not active in this version.

## 6. Permissions
The current app uses internet access for HTTPS communication with Google and required services. It does not currently request operational access to contacts, location, camera, or microphone.

## 7. Children
The developer must determine the app's target age group before release and align it with Google Play requirements.

## 8. Security
The app aims to limit permissions and keep information in private app storage. However, current local storage does not provide dedicated encryption at rest, and this development version has not been approved for general public release.

## 9. Changes to this policy
Before enabling online accounts, AI models, additional integrations, payments, or cloud synchronization, this policy must be updated to describe the new data flows.

## 10. Contact
For privacy questions, contact: [Support email]

---

**Release reminder:** This is only a draft based on the current source code. It is not legal advice. Before submitting to Google Play, ensure the developer identity, support email, public URL, final app behavior, and Data safety form all match exactly.
