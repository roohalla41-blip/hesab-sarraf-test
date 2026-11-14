# ATLAS AI — Product Roadmap

## Product foundation
- [x] Brand identity and Persian-first product direction
- [x] Android Kotlin + Jetpack Compose project scaffold
- [x] Provider-independent domain and repository contracts
- [x] Server API, privacy, and safety design notes
- [x] Feature catalog with explicit availability states
- [x] Interactive browser preview shell for all primary product areas

## Core product structure
- [x] Browser preview navigation for chat, history, explore, tools, profile, plans, settings, privacy, and help
- [x] First-pass Android Compose navigation drawer and screen shells for the same product areas
- [x] First-pass content and empty states for all primary sections
- [x] Preview-only theme and language controls
- [x] Clear notices for features that are not connected or operational
- [ ] Complete resource-based localization throughout Android UI
- [ ] Durable local-only conversation history and conversation management
- [ ] Full chat actions: copy, edit/resend, regenerate, stop, share, and feedback
- [ ] Attachment picker and secure local staging
- [ ] Accessibility and responsive-layout review
- [ ] UI tests and device-level review

## Backend integration (requires separate authorization)
- [ ] Select AI provider and model strategy
- [ ] Confirm backend project and data boundaries
- [ ] Configure server-side secrets
- [ ] Implement authenticated API and quota enforcement
- [ ] Add account lifecycle, cloud sync, and data deletion
- [ ] Add subscription and billing integration

## Advanced capabilities
- [ ] Streaming responses and message regeneration
- [ ] Image and document understanding
- [ ] Voice input and output
- [ ] Web research and coding tools
- [ ] Additional language packs and native review

## Release readiness
- [ ] Security and privacy review
- [ ] Accessibility and localization QA
- [ ] Automated tests and manual device testing
- [ ] Store listing, privacy disclosures, and support process
- [ ] Build and signed release only after explicit user approval

A roadmap item marked complete means a first-pass UI/navigation structure exists; it does not mean the feature is operational or connected to live services. No AI, database, authentication, billing, APK build, or deployment is included in this stage.
