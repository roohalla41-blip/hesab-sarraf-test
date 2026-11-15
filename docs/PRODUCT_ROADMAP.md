# ATLAS AI — Product Roadmap

## Product foundation
- [x] Brand identity and Persian-first product direction
- [x] Android Kotlin + Jetpack Compose project scaffold
- [x] Provider-independent domain and repository contracts
- [x] Server API, privacy, and safety design notes
- [x] Feature catalog with explicit availability states
- [x] Interactive browser preview shell for primary product areas
- [x] Adaptive Android launcher icon and monochrome icon

## Android product structure
- [x] Primary navigation and first-pass screens
- [x] Branded navigation drawer with grouped destinations
- [x] Guest profile screen with local-data disclosures
- [x] Persistent light/dark appearance preference
- [x] Multiple local conversations: create, open, continue, rename, search, and delete with confirmation
- [x] Migration from the previous single-conversation local store
- [x] Automatic FIFO retention of the newest 300 messages per conversation in local history
- [x] Keep one previous local history snapshot and recover it if the current JSON snapshot is malformed
- [ ] Execute and verify automated retention, recovery, and migration tests
- [x] Copy individual chat messages
- [x] Automatic Persian/English selection from device locale and RTL/LTR layout direction
- [x] Explicit user-facing state when the general AI engine is not connected
- [ ] Replace the bounded SharedPreferences conversation store with a transactional local database
- [ ] Extract all translations into Android resources and complete native localization review
- [ ] Complete Dari-specific terminology and proofreading
- [ ] Full chat actions: edit/resend, regenerate, stop, share, and feedback
- [ ] Attachment picker and secure local staging
- [ ] Accessibility and responsive-layout review
- [ ] UI tests and device-level review

## AI engine and backend (separate authorization required)
- [ ] Confirm the independent ATLAS backend destination and data boundaries
- [ ] Select provider/model routing and configure server-side secrets
- [x] Add authenticated Android HTTP adapter boundary (not yet wired)
- [ ] Connect Android client to authenticated server API
- [ ] Implement atomic quota, rate limiting, and abuse controls
- [ ] Add streaming responses, cancellation, and retry behavior
- [ ] Add account lifecycle, cloud sync, export, and deletion
- [ ] Add subscription entitlements and billing

## Integrations and tools
- [ ] Verify Gmail OAuth on physical Android devices and document OAuth verification requirements
- [ ] Implement GitHub, Drive, Calendar, and other approved integrations
- [ ] File and image understanding
- [ ] Voice input and output
- [ ] Web research and coding tools

## Release readiness
- [ ] Security and privacy review
- [ ] Accessibility and localization QA
- [ ] Automated tests and manual device testing
- [ ] Store listing, privacy disclosures, data-safety answers, and support process
- [ ] Release signing configuration and signed Android App Bundle (AAB)
- [ ] Google Play testing track and production rollout

A checked item means the described source/UI behavior exists; it does not imply that it has passed device, security, or production testing. No release is considered complete until all release gates have evidence.
