# MineHost Changelog

All notable MineHost changes are recorded here. Versions follow Semantic Versioning (MAJOR.MINOR.PATCH). Android `versionCode` is derived as `MAJOR * 1,000,000 + MINOR * 1,000 + PATCH`.

## [1.0.1] - 2026-11-10

### Changed
- Introduced a single root `VERSION` file as the source of truth for Android `versionName` and `versionCode`.
- Relay startup now reports a bounded timeout as an explicit error instead of remaining stuck in STARTING.
- Relay startup failure clears any public endpoint left over from an earlier session and explicitly preserves LAN-only hosting.
- Added actionable relay setup guidance to the timeout diagnostic.
- Corrected the Android `EACCES` diagnostic: the current Playit installer runs a downloaded executable from app-private writable storage, which Android 10+ may block.

### Known limitations
- The Playit Android executable launch path and current agent output compatibility still require physical-device verification. The current downloader stores the executable in app-private writable storage; Android execution policy may reject it. This version does not claim that public relay is fixed or production-ready.
- Phase 1 real second-client Bedrock gameplay acceptance remains dependent on a second physical Minecraft Bedrock client.

## [1.0.0]
- Initial internal app version before tracked release notes were introduced.
