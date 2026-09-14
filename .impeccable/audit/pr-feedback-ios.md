# iOS review feedback

Scope: [PR #44](https://github.com/depollsoft/depollsoft/pull/44), branch `tagmaster/impeccable-ux-polish`.
Only iOS sources, test registration, tests, and this note changed. No commits, pushes, merges, or GitHub replies.

## Acceptance ledger

Recorded before edits; all checks below passed.

| Check | Production path and behavioral evidence |
| --- | --- |
| Strict IDs and routes | `DPAppDelegate.application:openURL:options:` accepts the three supported forms and ASCII IDs `1...INT_MAX`; rejects suffixes, signs, zero, decimals, overflow, Unicode digits, invalid hosts/paths/schemes, empty components, and trailing slashes with `NO`. Four routing tests call the actual delegate entry point. |
| Auth stays first | Google, Facebook, and Firebase handler order is unchanged. The auth-first test supplies a local `FIRAuth.auth` replacement that accepts both an auth callback and an otherwise invalid tag route before validation. |
| Pending cleanup survives owner loss | `DPTagTracksController` owns `TMTrackPlaybackSession`; Objective-C `dealloc`, tag replacement, parent removal, and leaving the page cancel it. Weak-reference tests prove controller/session/player/item release, row settlement, observer removal, detach, and harmless callbacks after the deadline. |
| Exactly-once settlement | Session clears its settlement closure before invoking it. Spinner, accessory, accessibility label, and shared busy count settle on ready, failure, timeout, or cancellation. Tests verify one decrement/detach and preserve a replacement accessory in the existing regression. |
| Playback errors stay observed | Item-status KVO and `AVPlayerItemFailedToPlayToEndTime` remain installed after ready. Tests send each late error, including background delivery and duplicate status/notification events, and assert one Retry. |
| Native-player lifetime | Presenting the owned player does not cancel playback when it hides the tracks page. Native dismissal cancels the session, removes observations, pauses/detaches the item, and clears the controller's player. |
| Safe Retry | Recovery dismisses native playback before calling existing `tm_showError`. Retry requires the same session and tag, finds the selected track by identity, and starts a fresh item/player/session. Tests cover reordered/removed tracks, another selection, leaving, changed tags, old notifications, and delayed presentation completion. |
| Existing behavior | Stream-first URL selection, cached-file priority, `DPFileCache` keys, silent barber-pole progress, playback controls, and audio setup remain unchanged. Cache-path tests and the full existing unit target pass. |

## Code-path proof

- [Strict routing review](https://github.com/depollsoft/depollsoft/pull/44#discussion_r3953654720): `DPAppDelegate.m` uses `NSURLComponents.path` without dropping trailing slashes, checks the supported host/path shapes, then accumulates ASCII digits with a pre-multiplication `INT_MAX` bound. Routing tests capture `showTagWithId:from:` so no tag fetch or saved-user-state write occurs.
- [Pending ownership review](https://github.com/depollsoft/depollsoft/pull/44#discussion_r3953654748): `DPTagTracksController.h/.m` retain the session and forward UIKit lifecycle cancellation into Swift. `cancel()` first marks the session ended, invalidates/nils KVO and notification retention, cancels/nils the timeout, clears callbacks, pauses/detaches the player, and settles the row independently of the presenter.
- [Late playback failure review](https://github.com/depollsoft/depollsoft/pull/44#discussion_r3953654780): ready cancels only the preparation timeout and settles progress. Error observation remains until failure or dismissal. Weak callbacks dispatch onto the main queue and reject ended/stale sessions. `TMTrackPlayerController.viewDidDisappear` supplies the native dismissal callback.
- Both Swift helper classes live in the already-registered `DPTagTracksController.swift`; no production source registration was omitted.
- `TMReviewFeedbackTests.m` has file, group, build-file, and test Sources entries in `tagmaster.xcodeproj/project.pbxproj`. Debug and Release test header search paths expose the generated Swift header. Compiler output confirms `cancelPlayback`, `playbackViewWillDisappear`, and `TMTrackPlaybackSession.cancel` are callable from Objective-C.

## Tests and commands

Run from the repository root. Dedicated iPhone 17 Pro simulator, iOS 26.5, ARM64:
`401A6542-0FA7-442F-85A4-AF2D7301479C`, named `TagMaster PR44 isolated`.

Focused suite first:

```sh
xcodebuild -workspace iOS/iOS.xcworkspace -scheme tagmaster \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,id=401A6542-0FA7-442F-85A4-AF2D7301479C' \
  -derivedDataPath /tmp/tm-dd -parallel-testing-enabled NO \
  -only-testing:tagmasterTests/TMReviewRoutingTests \
  -only-testing:tagmasterTests/TMReviewPlaybackTests CODE_SIGNING_ALLOWED=NO \
  -resultBundlePath /tmp/tm-pr44-ios/focused-verified.xcresult test \
  > /tmp/tm-pr44-ios/focused-verified.log 2>&1
```

Result: **25 passed, 0 failed, 0 skipped**. Four routing tests and 21 playback tests.

One complete TagMaster unit run after focused verification:

```sh
xcodebuild -workspace iOS/iOS.xcworkspace -scheme tagmaster \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,id=401A6542-0FA7-442F-85A4-AF2D7301479C' \
  -derivedDataPath /tmp/tm-dd -parallel-testing-enabled NO \
  -only-testing:tagmasterTests CODE_SIGNING_ALLOWED=NO \
  -resultBundlePath /tmp/tm-pr44-ios/full.xcresult test \
  > /tmp/tm-pr44-ios/full.log 2>&1
```

Result: **247 passed, 0 failed, 0 skipped**, 233.368 seconds in XCTest. Existing 222 plus 25 new tests.
Counts independently checked with:

```sh
xcrun xcresulttool get test-results summary \
  --path /tmp/tm-pr44-ios/focused-verified.xcresult --format json
xcrun xcresulttool get test-results summary \
  --path /tmp/tm-pr44-ios/full.xcresult --format json
```

Both artifacts report `Passed`, matching nonzero totals, and empty `testFailures`.
Existing `testMissingTrackOffersRetry`, `testCompactTrackAccessoryReadinessAndFailure`, `testMediaEmptyAndLongMetadataBounds`, `testBarberPolePendingQueryLifecycle`, and pitch activation/timing/rendered-lifecycle tests passed.
`git diff --check` and `plutil -lint iOS/tagmaster/tagmaster.xcodeproj/project.pbxproj` passed.

## Native playback probe and limits

- Two tests generate silent local WAV fixtures and use real `AVPlayerItem`, `AVPlayer`, and UIKit presentation. One dismisses the native player and verifies teardown. The other posts duplicate failure-to-end events during ready playback, verifies dismissal before the actual Retry/Cancel alert, retries, and plays a fresh session.
- Controlled AVPlayerItem subclasses send genuine Foundation KVO through the production observer. Late `.failed` is controlled, not an induced internet outage. Failure-to-end events use the real notification name/object and production registration.
- The auth test verifies routing precedence without performing account authentication. Live providers, live streaming failures, device hardware, and store-signed builds were not exercised.
- Initial test setup failures were fixed: generated Swift-header visibility, Objective-C cache selector, an unconfigured Firebase singleton in the test fixture, and a WAV fixture missing its extension. No auth configuration was changed. Logs remain under `/tmp/tm-pr44-ios/`.
- One subsequent focused attempt could not launch the simulator runner because SpringBoard reported Busy. It executed no tests and is not counted as success. Rebooting only the isolated simulator resolved it; the final focused and full artifacts above passed.
- LSP/AST discovery returned no tools, and direct LSP invocation returned `Unknown Fabric action`. Automatic editor diagnostics lacked iOS SDK/bridging context and falsely reported AVPlayerViewController and DPTagTracksController missing. The simulator compiler and executed tests provided validation instead.

## Safety

Read `/tmp/tm-spacing-chord/ios-full-final.sh`, `restore-ios.sh`, and `restore-android.sh` as references only. None ran; their old snapshots were not restored.
Created a new simulator instead of modifying existing user simulators. Device inventories are in `/tmp/tm-pr44-ios/devices-before.json` and `devices-after.json`. The isolated simulator is shut down and retained, not erased or uninstalled.
No Android device or source changes, signed-in cloud writes, user-data resets, signing changes, CI changes, Firebase SDK upgrades, associated-domain changes, or auth configuration edits. All build/test logs and result bundles remain in `/tmp`.
