# ATLAS AI — Product Roadmap

## Product foundation
- [x] Brand identity and Persian/Dari-first product direction
- [x] Android Kotlin + Jetpack Compose project
- [x] Provider-independent domain and repository contracts
- [x] Server API, privacy, and safety design notes
- [x] Feature catalog and explicit availability states
- [x] Browser interface for primary product areas
- [x] Adaptive Android launcher and monochrome icons
- [x] Main navigation, guest profile, settings, and light/dark appearance
- [x] Automatic Persian/English locale selection and RTL/LTR direction

## Chat and history
- [x] Android chat screen sends requests to the deployed `atlas-chat` Edge Function
- [x] Browser chat sends requests to the same Edge Function
- [x] Clear pending, success, and failure states in chat
- [x] Local multi-conversation history on Android
- [x] Create, open, continue, rename, search, and delete local conversations
- [x] Bounded local retention (100 conversations; 300 messages each)
- [ ] Replace SharedPreferences JSON storage with a transactional local database
- [ ] Persistent browser history and optional cross-device sync, only after privacy/data design approval
- [ ] Edit/resend, regenerate, stop generation, share, and message feedback
- [ ] Streaming responses, cancellation, and safe retry behavior
- [ ] Accessibility, dynamic text sizing, and physical-device layout review
- [ ] Full localization resource extraction and native Dari proofreading

## AI backend and security
- [x] Deploy the `atlas-chat` Edge Function (active version 9 at last inspection)
- [x] Configure Android and browser clients to call the function endpoint
- [x] Keep provider credentials on the server
- [x] Provider fallback code for OpenAI-compatible, Groq, Z.AI, and Cloudflare Workers AI
- [ ] Confirm that Supabase project `wwlyvojtsrklcpqsloqz` is the intended ATLAS-only backend. Its public schema currently contains 59 exchange-office, wallet, payment, and user-related tables; reported row counts were zero at inspection. No database changes have been made.
- [ ] Confirm provider Secret names/model values and verify a real successful model response
- [ ] Select and implement user authentication; the current endpoint accepts requests without a user JWT
- [ ] Add durable per-user quotas, atomic usage accounting, and abuse-resistant rate limiting
- [ ] Add monitoring and operational alerts without logging private prompts or secrets
- [ ] Add account lifecycle, data export/deletion, and optional cloud sync after backend identity is confirmed

## Integrations and tools
- [x] Gmail read-only integration code path
- [ ] Verify Gmail OAuth consent and read-only inbox retrieval on physical Android devices
- [ ] Complete Google OAuth verification if public Gmail access is retained
- [ ] Implement GitHub, Drive, Calendar, and other integrations only with explicit user authorization
- [ ] File/document upload and understanding
- [ ] Image understanding and generation
- [ ] Voice input and output
- [ ] Web research and source citations
- [ ] Coding workspace and controlled tool execution

## Plans and payments
- [ ] Define free/paid entitlements and usage limits
- [ ] Select payment providers and implement billing only after product and region requirements are confirmed
- [ ] Add subscription restore, cancellation, and billing support

## Release readiness
- [ ] Resolve backend/database identity blocker
- [ ] Complete security and privacy review
- [ ] Complete accessibility and localization QA
- [ ] Run automated unit/UI tests and physical-device tests
- [ ] Finalize privacy policy, support process, and store disclosures
- [ ] Prepare Google Play store listing, icon, feature graphic, and real app screenshots
- [ ] Receive the user's release keystore and configure protected CI secrets
- [ ] Build and verify a signed Android App Bundle (AAB)
- [ ] Test through Google Play internal/closed tracks and satisfy account-specific requirements
- [ ] Production rollout and post-launch monitoring

A checked item means the described source or deployed configuration exists; it does not mean that the feature has passed live model, security, or physical-device validation. ATLAS is not production-ready until every release blocker has evidence.
