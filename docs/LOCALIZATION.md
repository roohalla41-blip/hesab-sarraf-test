# ATLAS AI — Localization

## Current Android behavior
- The Android UI selects Persian when the device language is Persian (fa) or Dari (prs).
- Other device languages currently use the English fallback.
- Layout direction follows the device locale: right-to-left for RTL locales and left-to-right otherwise.
- User-authored chat content is preserved as entered and is not automatically translated.
- Gmail metadata labels and system status messages follow the same device-language selection.

## Current limitations
- The current translation mapping is centralized in AtlasText; it is not yet fully extracted into Android resource files.
- Dari currently uses the Persian text set; Dari-specific terminology and proofreading remain pending.
- Date, number, and time formatting still needs a locale-aware review.
- Switching the device language while the app is running may require restarting the app.

## Release quality
Before release, extract all interface text into Android resources, verify every screen in Persian and English, review RTL/LTR layouts on physical devices, add native Dari translations if Dari is advertised, and complete accessibility and native-language proofreading.
