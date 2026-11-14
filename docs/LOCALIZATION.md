# ATLAS AI — Localization Plan

## First supported interface languages
- Persian (fa): initial right-to-left interface.
- English (en): left-to-right interface and international fallback.
- Dari (prs): planned as a distinct language option, with terminology reviewed by native speakers.

## Rules
- Use Android resource qualifiers for translated interface text.
- Determine layout direction from the selected language, not from a hard-coded device assumption.
- Keep user-generated message content unchanged; do not auto-translate it without consent.
- Store language preferences as BCP-47 tags.
- Format dates, numbers, and currencies with locale-aware APIs.
- Avoid concatenating translated fragments; use complete translatable sentences and placeholders.
- Keep product brand names such as ATLAS AI unchanged across languages.

## Release quality
Every supported language needs a complete string-resource audit, RTL/LTR layout review, accessibility review, and native-language proofreading before release. A language is not considered fully supported merely because a resource folder exists.
