# Tag Master audit and UX improvements

Fable audited Android and iOS. Astra implemented the fixes and subsequent review repairs after Fable's provider limit. Changes remain uncommitted.

## Delivered

- Android: native toolbar/Up navigation and tabs, legible light/dark controls, accessible home actions and rating/pitch/media controls, discoverable Teachable Tags, saved-row options, improved search/settings and error feedback.
- Android correctness: feed dates instead of false epoch dates, distinct PDF pages, validated HTTP/HTTPS tag routing, background cache reads, safer image loading and playback lifecycle.
- iOS: anchored iPad sharing, recoverable errors, named controls and pitch activation, Dynamic Type and self-sizing rows, usable large-text filters, discoverable Teachable Tags and scrolling track/video content.
- iOS review repair: stable intrinsic Summary measurements after page changes. Lost's lyrics remain 64.3pt high rather than growing to 350.3pt; heading-to-body offset is 0pt. Tested normal and maximum accessibility text at phone/iPad widths.
- Existing terminology, attribution, saved-list formats, optional account/sync contracts and share text retained. No deployments or association-file publication.

## Acceptance evidence

| Check | Result |
| --- | --- |
| Tag Master Android and iOS builds | Pass |
| Pitch Perfect Android shared-library compatibility build | Pass |
| Android unit tests | 262 pass across 23 classes |
| Android UI tests | All 105 current tests have passing results across the full run and final targeted repair runs. Last full run was 102/105; all three remaining test-isolation failures were repaired and their complete classes passed 27/27. No claim of a single final 105/105 run. |
| iOS selected unit regressions | Initial 45 passed; final 14-test run passed all 12 polish tests and 2 additional Summary layout tests. 47 distinct selected tests covered across runs, not the entire iOS suite. |
| iOS native journeys | Phone and iPad sharing/rating, screen journeys, dark maximum-text checks passed. Final lyrics reachability passed on both devices. |
| Visual review | Native simulator/emulator captures inspected. Known Summary spacing defect fixed with measured regression probes after the bounded review. |
| Source hygiene | CRLF-aware git diff --check passes. New native test registrations verified. |
| Targeted main-session LSP | Android routing/pitch files clean. iOS clang reported a stale Pods module-map reference; the actual SPM/Xcode build and selected tests passed. |
| Device restoration | Workers recorded and restored device settings; Android test repair also restored original APK and private data with byte comparison. |

The temporary agent-created Android/.pi-lens.json formatting override was removed during final cleanup. It is not part of the product changes.

## Limits and follow-up

No complete hardware VoiceOver/TalkBack session, audible playback certification, live sign-in/cloud-sync exercise, full iOS suite, or hardware performance audit. iOS blocking network infrastructure remains. Android expanded-width layout is still single-column; full tablet/list-detail redesign and saved-list RecyclerView migration were deferred. Android App Links and iOS Universal Links are not verified: hosted association files and release-signing configuration need separate work. Original audit scores are baseline findings, not post-fix scores.

## Detailed records

- `.impeccable/audit/2026-09-07__tagmaster-android.md`
- `.impeccable/audit/2026-09-07__tagmaster-ios.md`
- `.impeccable/audit/verification-android.md`
- `.impeccable/audit/verification-ios.md`
- Native captures under `.impeccable/review/tagmaster-*-after-*.png`.
