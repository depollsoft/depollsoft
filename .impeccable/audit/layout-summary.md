# Tag Master layout pass

## Scope and approach

Reviewed every Tag Master screen family on Android and iOS using independent spatial and mechanical assessments, current source, recent captures and targeted native fixtures. This is refinement, not a new visual identity. The screen matrices in `layout-assessment.md`, `layout-android.md` and `layout-ios.md` distinguish rendered, source-reviewed and unverified states.

## Changes

- Android empty Tracks now leads with the explanation and retains recording notes, without unusable transport controls. Populated-to-empty transitions stop and clear media before hiding controls. Playable, pending and failed-media states keep the full player and parts.
- Android initial tag-load errors can scroll in short windows, keeping the complete message and Retry reachable.
- iOS Details adapt caption/value pairs to actual width and text size. AX5 tag IDs no longer become one-character columns.
- iOS Search and Settings use their existing menu controls when segments cannot fit usable targets, not only at accessibility text sizes.
- iOS Summary has one conditional section break before Lyrics/Notes.
- iOS empty Teachable guidance and Browse action scroll at large text sizes. The sheet-preview Key control has a real 44pt target without enlarging the navigation bar or changing playback behavior.
- iOS now uses a real UITabBarController. UIKit owns the glass capsule, selected lens, margins, type sizing and accessibility. Four destinations stay at the bottom of phone/iPad panes. Android remains full width. The rejected custom glass-backed button strip is removed.
- Native tab visibility is changed through UIKit's controller API so loaded content receives the correct safe-area inset.

Preserved: charcoal navigation, blue actions, green availability, compact credits, opaque Android sheet-key surface, pressed feedback, shared vector/motion definitions, optional accounts and all cache/API/persistence/link contracts.

## Verification

- Android full unit run: 300 tests pass. Final targeted native repair run: 12 tests pass across normal/large/landscape/expanded dialog, query, playback and reachability cases. Earlier screen-family tests and limits are detailed in `layout-android.md`.
- iOS final complete unit target: 218/218 pass. Scoped native journeys verify real glass selection, rotation, Teachable Browse, rating-choice reach and sheet-key targeting on phone/iPad. The full UI suite was not rerun.
- Layout detector before/after: exit 0, `[]`. Its native coverage is unproven; source measurements and native tests provide the actual evidence.
- Both generated-artwork checks, project/source registrations and whitespace checks pass. Shared art and other-app libraries are unchanged.
- Original apps, private data and settings restored; backups retained under /tmp.

Several initial failures were fixture-coordinate/readiness/native-scroll assumptions, not production bugs. They were diagnosed and their assertions corrected without skips or expected failures. The final integrated iOS run also caught and fixed an actual pending-to-loaded safe-area bug.

## Evidence and limits

The PR includes selected Android fix/coverage screenshots, native iOS sheet-key captures and the final UIKit glass tabs. Broader historical capture inventories in the assessment reports remain local evidence; not every intermediate image is included in the commit.

Private-build Settings, third-party authentication internals, full-document pan/zoom and physical-device assistive/performance behavior are not certified. iOS 17 fallback is compile-checked, not runtime-tested. Layout assessment covers all screen families, not every possible device/state combination.
