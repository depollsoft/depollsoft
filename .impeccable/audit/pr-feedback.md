# Pull request feedback

Review: [#44](https://github.com/depollsoft/depollsoft/pull/44).

## Acceptance ledger

- Android dates must reject impossible calendar values and trailing data, while retaining feed/ISO/US dates and signed millisecond timestamps. Test the parser and XML ingestion.
- iOS deep links must consume the full positive, in-range tag ID before routing. Preserve supported URL forms and authentication callbacks.
- iOS pending track preparation must cancel and release observations/player resources when its owner leaves.
- Late playback errors, including failure-to-play-to-end notifications, must reach Retry exactly once and release observations on dismissal.
- Correct the audit summary's obsolete instrumentation result and completed-layout/list deferrals using the round-two evidence.
- Reply to and resolve review threads only after verified fixes are pushed. Merge after the checks pass.

The iOS implementation and test evidence are recorded in `pr-feedback-ios.md`.

## Verified results

- [x] Android now uses non-lenient parsing with `ParsePosition` and requires full-input consumption. Four added parser tests cover impossible dates, suffixes, leap days and signed timestamp boundaries. XML ingestion tests also reject malformed Posted values.
- [x] All 17 focused parser/XML tests passed. The full Android unit target passed 319 tests with zero failures/errors/skips; the TagMaster APK built successfully. Logs: `/tmp/tagmaster-review-dates.log`, `/tmp/tagmaster-review-full-android.log`.
- [x] iOS validates supported routes and ASCII IDs in `1...INT_MAX`, after authentication handlers. An owned playback session cancels pending work, observes errors through playback, cleans up on dismissal and offers stale-safe Retry.
- [x] All 25 new iOS tests passed. The full iOS unit target passed 247 tests with zero failures/skips, verified through the xcresult summary. Local native playback and recovery probes passed on an isolated simulator. See `pr-feedback-ios.md` for commands and limits.
- [x] The audit summary now records round two's complete 105/105 Android instrumentation run and 167/39 iOS unit/UI runs. It no longer defers the RecyclerView migration or the delivered landscape layouts. Original round-one records remain historical evidence.
- [x] Coordinator reviewed URL bounds, observer ownership, timeout cancellation, UIKit dismissal and Retry identity checks. New source/test registration and `git diff --check` passed. The Swift file's historical attribution header is preserved.

Manual live streaming outages, live authentication and hardware testing were not performed. Final remote CI and merge status are recorded on the pull request.
