# Tag Master audit and UX improvements

Fable audited Android and iOS. Astra implemented the fixes and subsequent review repairs after Fable's provider limit. Changes are published in [#44](https://github.com/depollsoft/depollsoft/pull/44). This summary includes the completed round-two verification; later refinements have separate reports.

## Delivered

- Android: native toolbar/Up navigation and tabs, legible light/dark controls, accessible home actions and rating/pitch/media controls, discoverable Teachable Tags, saved-row options, improved search/settings and error feedback.
- Android correctness: feed dates instead of false epoch dates, distinct PDF pages, validated HTTP/HTTPS tag routing, background cache reads, safer image loading and playback lifecycle.
- iOS: anchored iPad sharing, recoverable errors, named controls and pitch activation, Dynamic Type and self-sizing rows, usable large-text filters, discoverable Teachable Tags and scrolling track/video content.
- iOS review repair: stable intrinsic Summary measurements after page changes. Lost's lyrics remain 64.3pt high rather than growing to 350.3pt; heading-to-body offset is 0pt. Tested normal and maximum accessibility text at phone/iPad widths.
- Existing terminology, attribution, saved-list formats, optional account/sync contracts and share text retained. No deployments or association-file publication.

## Audit and round-two acceptance evidence

These are the completed audit/round-two results, not a fresh run of every suite after each later refinement. Subsequent layout, loading and saved-list checks have their own reports.

| Check | Result |
| --- | --- |
| Tag Master Android and iOS builds | Pass |
| Pitch Perfect Android shared-library compatibility build | Pass |
| Android unit tests | 262 pass across 23 classes |
| Android UI tests | Round two completed a single full 105/105 instrumentation run with zero failures, errors or skips; see `round2-android.md` and `/tmp/r2-connected.log`. This supersedes the initial 102/105 run documented in `verification-android.md`. |
| iOS unit tests | Round two completed the full 167-test target with zero failures; see `round2-ios.md`. |
| iOS native journeys | Round two completed all 39 UI tests with zero failures. Phone/iPad and maximum-text probes are also recorded in the platform reports. |
| Visual review | Native simulator/emulator captures inspected. Known Summary spacing defect fixed with measured regression probes after the bounded review. |
| Source hygiene | CRLF-aware git diff --check passes. New native test registrations verified. |
| Targeted main-session LSP | Android routing/pitch files clean. iOS clang reported a stale Pods module-map reference; the actual SPM/Xcode build and selected tests passed. |
| Device restoration | Workers recorded and restored device settings; Android test repair also restored original APK and private data with byte comparison. |

The temporary agent-created Android/.pi-lens.json formatting override was removed during final cleanup. It is not part of the product changes.

## Limits and follow-up

No complete hardware VoiceOver/TalkBack session, audible playback certification, live sign-in/cloud-sync exercise or hardware performance audit. Some iOS catalog helpers still run synchronously on background queues. Android Favorites and Teachable Tags now use RecyclerView, and round two delivered two-column landscape Summary/Tracks layouts that later passes refined. A full Android tablet/list-detail redesign remains deferred. Android App Links and iOS Universal Links are not verified: hosted association files and release-signing configuration need separate work. Original audit scores are baseline findings, not post-fix scores.

## Detailed records

- `.impeccable/audit/2026-09-07__tagmaster-android.md`
- `.impeccable/audit/2026-09-07__tagmaster-ios.md`
- `.impeccable/audit/verification-android.md`
- `.impeccable/audit/verification-ios.md`
- Native captures under `.impeccable/review/tagmaster-*-after-*.png`.
