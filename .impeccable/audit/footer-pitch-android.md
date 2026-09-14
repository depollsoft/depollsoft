# Android footer and pitch corrections

## Result

Home credits now share compact wrapping rows. Summary and sheet Key controls fill blue while their actual note is playing, with on-primary text and icons. Idle, release, cancel and completion return to the outline without changing geometry.

## Acceptance ledger

- [x] BodySmall credits, 8dp top padding, one provider link, shared credit/Terms/Donate row, wrapping whole link groups.
- [x] Four disjoint link targets at least 48dp; original five destinations, app/version/copyright IDs, version resource, © 2021 and hidden promo preserved.
- [x] Native rendered fill, text pixels and icon pixels checked in light/dark with contrast at least 4.5:1.
- [x] Hold, release, cancel, disabled, timed accessibility click, toggle, replacement and detach covered. Shared PitchPipeButton and synth unchanged.
- [x] Favorite Home captures in light/dark and font 2.0; Summary and sheet before/held/released captures.
- [x] Original installed APK restored. Private app files compare byte-for-byte with the backup; recorded device settings match exactly. No generated favorites remain.

## Measured footer height

Native Android 16, 360dp width:

| Font scale | Before | After |
| --- | ---: | ---: |
| 1.0 | 262.86dp | 128.38dp |
| 1.3 | 274.29dp | 133.33dp |
| 2.0 | 293.33dp | 204.57dp |

Unit bounds checks also cover 320dp and 600dp. Terms and Donate share a row at default sizes.

## Verification

Across focused runs, 80 distinct unit cases passed: 63 existing interaction/brand/layout/query cases, eight new footer/pole cases and nine existing quartet-loading cases. Debug and instrumentation APK builds passed under JDK17. Native footer/pitch tests passed in light, dark and large-text variants. Logs are under `/tmp/tagmaster-footer-pitch`, including `final-native-light.log`, `final-native-dark-summary.log`, `final-native-large.log` and `final-build-quartet.log`.

The initial pressed selector exposed Android's queued touch state reasserting after note replacement. Local Activated now follows Note.IsPlaying directly. No sound behavior was added.

Captures: `.impeccable/review/tagmaster-android-footer-pitch-*.jpg`, maximum dimension 800px. Review used one combined batch and one pole-evidence confirmation. Emulator only; native note fixtures use a silent player, with existing lifecycle tests checking play/stop calls. LSP tools were unavailable; Kotlin checks, compilation and tests were used. No commits or pushes.
