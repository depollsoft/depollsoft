# Tag Master iOS verification

Astra implementation, 2026-09-07. iOS only. No commits or deployment. Android changes belong to the other agent.

## Acceptance results

| Check | Result | Evidence |
| --- | --- | --- |
| iPad share presentation and dismissal | Pass | Share uses its actual UIBarButtonItem anchor. Native UI tests present and dismiss the system activity collection, then open Rating without crashing. Tested default light and dark AX5 on iPad and phone. |
| Rating popover | Pass | sourceView and sourceRect match the Rate button; all five spoken star choices checked by unit tests; native presentation/dismissal checked. |
| Recoverable failures | Pass in deterministic regression coverage | Nil tag load, failed refresh with retained tag, random exception/empty result, query exception/exhausted refresh, rating failure/retry, missing sheet music/retry to Quick Look, missing track/retry. Busy counters return to zero. Real recovery alert has Retry/Cancel. Nil sheet data is not cached. |
| Named controls | Pass | Search, Share, Favorite and Teachable options, Refresh, filters, Rate tag and Play key note names checked. Local VoiceOver activation calls timed pitch playback; TouchCancel stops pitch. No live VoiceOver session or audible hardware check. |
| Dynamic Type and row sizing | Pass for tested rows | Tag rows, track rows/notes and video metadata self-size. Native maximum-text browse cells are between 100 and 2,000pt. Long-title and availability semantics tested. Existing query parameters and list operations remain intact. |
| Important target sizes | Partial | Native detail tabs and content/filter/pitch controls meet the tested 44pt minimum. UINavigationBar system image items retain UIKit sizing; their AX frames reported 36pt, and UIKit's effective hit margins were not independently measured. |
| Teachable discovery and empty state | Pass by source/unit checks | Entry always exists; empty state explains Mark as Teachable, provides Browse Tags, and disables empty-list editing. Favorites get empty guidance. Teachable empty state was not separately screenshot-inspected. |
| Screenshots and bounded visual review | Completed | One review batch and one confirmation batch. No further captures or visual reviews. The residual Summary spacing defect was subsequently fixed and proved with deterministic frames and native reachability tests, documented below. |
| Build, source contracts and restored settings | Pass | Requested Debug build; targeted tests; git diff --check; protected-file and literal-contract comparisons; both simulators restored to light/large. |

## What changed

All production edits are under `iOS/tagmaster/tagmaster/`. Tests are in existing registered source files under `iOS/tagmaster/tagmasterTests/` and `iOS/tagmaster/tagmasterUITests/`. The existing UI test target was added to the `tagmaster` scheme.

- Anchored share/rate popovers and disabled tag actions until a tag exists.
- Added user-facing Retry/Cancel errors without raw exception details or silent navigation pops. Preserved stale detail content on failed refresh.
- Added accessible names, timed pitch activation and local semantic pitch/rating colors.
- Added wrapping, self-sizing saved/query/track/video rows and accessible availability descriptions.
- Replaced only cramped search/settings field layout with vertical stacks. At accessibility text sizes, native menu buttons update the same underlying segmented indices and defaults mappings.
- Retained native detail/browse page tabs, giving them enough height instead of replacing navigation.
- Moved recording notes into a scrolling track-table header so large text cannot eliminate the track list.
- Made Teachable Tags discoverable when empty; enlarged content/link/footer controls.

No shared `depolllib` or Pitch Perfect library changes. Pitch Perfect build was therefore not required by this task and was not run.

## Successful commands and counts

All xcodebuild output was redirected to `/tmp` logs. Ordinary failures were captured with `settle:true`; simulator-restoration commands retained the original test exit status.

Build from `iOS/`, exit 0:

```sh
xcodebuild -workspace iOS.xcworkspace -scheme tagmaster -configuration Debug \
  -destination 'platform=iOS Simulator,id=C8B74E44-94F7-4CED-A47F-DFF98E34237B' \
  -derivedDataPath /tmp/tm-dd build > /tmp/tagmaster-ios-final-build.log 2>&1
```

Test commands use that workspace/scheme/configuration/derivedDataPath, `-parallel-testing-enabled NO`, and the device destination. Final phone light run used:

```sh
xcodebuild -workspace iOS.xcworkspace -scheme tagmaster -configuration Debug \
  -destination 'platform=iOS Simulator,id=C8B74E44-94F7-4CED-A47F-DFF98E34237B' \
  -derivedDataPath /tmp/tm-dd -parallel-testing-enabled NO \
  -only-testing:tagmasterTests/TMPolishRegressionTests \
  -only-testing:tagmasterTests/TagmasterAppLogicTests \
  -only-testing:tagmasterTests/DPSearchViewControllerTests \
  -only-testing:tagmasterUITests/TagMasterPolishUITests \
  -resultBundlePath /tmp/tagmaster-ios-complete-phone-light.xcresult test \
  > /tmp/tagmaster-ios-complete-phone-light.log 2>&1
```

| Final check | Count | Result/log |
| --- | --- | --- |
| Phone light unit suite | 45 tests: 12 new polish, 28 existing search, 5 existing app logic | 0 failures; `/tmp/tagmaster-ios-complete-phone-light.log` |
| Phone light native screen journey | 1 test | 0 failures; same log |
| Phone dark AX5 native screen journey | 1 test | 0 failures; `/tmp/tagmaster-ios-complete-phone-ax5.log` |
| iPad dark AX5 native screen journey | 1 test | 0 failures; `/tmp/tagmaster-ios-complete-ipad-ax5.log` |
| iPad light native screen journey | 1 test | 0 failures; `/tmp/tagmaster-ios-final-ipad-light.log` |
| iPad share-anchor unit check | 1 test | 0 failures; `/tmp/tagmaster-ios-final-ipad-ax5.log` |
| Phone AX5 lyrics after changing pages | 1 test | 0 failures; `/tmp/tagmaster-ios-lyrics-reachability.log` |
| iPad AX5 lyrics after changing pages | 1 test | 0 failures; `/tmp/tagmaster-ios-ipad-lyrics-reachability.log` |

The four screen-journey runs repeat the same test, not four distinct UI test methods. The lyrics check is a second UI method. Screen journeys visit home, search/keyboard, settings/filters, Summary, Details, Tracks and Videos, sharing, rating, and browse rows. Real tag 1809 and catalog queries were used; content availability remains network/cache-dependent. No ratings were submitted by UI tests and no share destination was invoked.

AX5 runs used `xcrun simctl ui <UDID> appearance dark` and `content_size accessibility-extra-extra-extra-large`, then `test-without-building` with `-only-testing:tagmasterUITests/TagMasterPolishUITests`. Reachability runs selected only `testSummaryLyricsRemainReachableAfterChangingPages` and produced no additional screenshot review round.

## Actual failures encountered and resolved

- `/tmp/tagmaster-ios-unit.log`, exit 65: direct Quick Look class reference did not link in the test target. Changed the test to runtime class lookup.
- `/tmp/tagmaster-ios-unit-2.log`, exit 65: 10 tests, 3 failures. Shared star-grid loading overlay caused 200,073pt rows, fixed locally. Phone popover expectation and overlong local-URL cache fixture were corrected.
- `/tmp/tagmaster-ios-phone-tests.log`: 43 unit tests passed; UI failed looking for Copy as a Button. System Copy uses different accessibility semantics.
- `/tmp/tagmaster-ios-phone-dark-large.log` and `...ipad-dark-large.log`: each UI journey failed because Copy moves behind View More at AX5. Tests now require the actual system activity collection, presentation and successful dismissal rather than a particular system action arrangement.
- `/tmp/tagmaster-ios-phone-confirm-dark.log`: 44 unit tests had 7 assertions fail across 2 tests due to pending trait changes and measuring hidden segmented controls. Updated traits before assertions and measured visible native menus. The UI test exposed the real track-page notes layout failure.
- `/tmp/tagmaster-ios-phone-final-dark.log`: 2 corrected unit tests passed; track UI still failed. The hierarchy disproved an empty-state hypothesis: Recording Notes consumed the viewport. Moved notes inside the scrolling table.
- `/tmp/tagmaster-ios-phone-verified-dark.log`: 11 unit tests passed; share dismissal tapped the system home-gesture area, leaving Rate covered. The final test taps outside the popover and explicitly waits for the activity collection to disappear.

No unresolved build/test failure in the final selected checks. LSP tooling was unavailable: `extensions.lsp_navigation` returned Unknown Fabric action; discovery found no `lsp_diagnostics` action. Compiler and XCTest checks were used instead, not reported as LSP success.

## Preserved contracts

Mechanical comparisons passed for Home/Query DPTag query arguments, Search/Settings defaults keys, AppDelegate launch guard and callback/URL handling, and DPTag content before the rating method. `git diff --quiet` confirmed no changes in shared libraries, Pitch Perfect, account/sync Swift implementations, Info.plist or entitlements. `git diff --check -- iOS` passed.

Preserved the handwriting title and barber-pole background; music terminology/parts; all links and share text; DPTag.tagUri; API parameters/field lists/cache flags; APP_VERSION/cache formats; favorites/teachable persistence, order and optional sync; original provider callbacks and schemes; XCTestCase guard and --uitesting. Fable's report and its Must NOT change list were not edited.

## Remaining defects and deferrals

- Standard navigation-bar icon hit margins need a dedicated accessibility measurement. Content controls and page tabs have explicit tested minimum sizes; do not read that as every UIKit AX frame being 44pt.
- Slow requests still use existing blocking loading infrastructure. No broad URLSession/streaming/timeout migration. Error-path tests do not prove every server-side rating response is accepted or every remote media file plays.
- Associated domains, entitlement/privacy cleanup and deep-link expansion are deferred. No association files were deployed, hosted AASA files were not verified, and Universal Links/App Links are not claimed working.
- Account/sync implementations were preserved, but live provider sign-in and cloud synchronization were not exercised.
- Simulator portrait coverage only. Landscape, hardware VoiceOver, audible playback, increased contrast, hardware performance and a full end-to-end catalog/media suite remain unverified. Semantic text colors were inspected visually; no complete numerical contrast audit was performed.

## Device restoration

Recorded before changes with `simctl ui appearance` and `content_size`:

- Phone `C8B74E44-94F7-4CED-A47F-DFF98E34237B`: light, large.
- iPad `55A9555A-9714-4FE0-8F0D-1035652526F9`: light, large.

Both were restored to light/large and queried again. Orientation, contrast and other simulator settings were not changed. Test fixtures and real catalog content were used on simulators; existing unit tests reset their defaults as designed.

## Capture method

Native `XCUIScreen.main.screenshot()` attachments, exported with `xcrun xcresulttool export attachments --path <xcresult> --output-path <directory>`. Each was reduced before inspection with `sips -Z 900 <source> --out <path>` and read individually through Pi. Files named `phone-dark` and `ipad-dark` use AX5. Light captures use the default `large` category. Raw native attachments and test logs remain under `/tmp`.

## Screenshot inventory

- `.impeccable/review/tagmaster-ios-after-ipad-dark-browse-rows.png`
- `.impeccable/review/tagmaster-ios-after-ipad-dark-detail.png`
- `.impeccable/review/tagmaster-ios-after-ipad-dark-home.png`
- `.impeccable/review/tagmaster-ios-after-ipad-dark-rating.png`
- `.impeccable/review/tagmaster-ios-after-ipad-dark-search-keyboard.png`
- `.impeccable/review/tagmaster-ios-after-ipad-dark-search.png`
- `.impeccable/review/tagmaster-ios-after-ipad-dark-settings-filters.png`
- `.impeccable/review/tagmaster-ios-after-ipad-dark-settings.png`
- `.impeccable/review/tagmaster-ios-after-ipad-dark-share.png`
- `.impeccable/review/tagmaster-ios-after-ipad-dark-tracks.png`
- `.impeccable/review/tagmaster-ios-after-ipad-dark-videos.png`
- `.impeccable/review/tagmaster-ios-after-ipad-light-browse-rows.png`
- `.impeccable/review/tagmaster-ios-after-ipad-light-detail.png`
- `.impeccable/review/tagmaster-ios-after-ipad-light-home.png`
- `.impeccable/review/tagmaster-ios-after-ipad-light-rating.png`
- `.impeccable/review/tagmaster-ios-after-ipad-light-search-keyboard.png`
- `.impeccable/review/tagmaster-ios-after-ipad-light-search.png`
- `.impeccable/review/tagmaster-ios-after-ipad-light-settings-filters.png`
- `.impeccable/review/tagmaster-ios-after-ipad-light-settings.png`
- `.impeccable/review/tagmaster-ios-after-ipad-light-share.png`
- `.impeccable/review/tagmaster-ios-after-phone-dark-browse-rows.png`
- `.impeccable/review/tagmaster-ios-after-phone-dark-detail.png`
- `.impeccable/review/tagmaster-ios-after-phone-dark-home.png`
- `.impeccable/review/tagmaster-ios-after-phone-dark-rating.png`
- `.impeccable/review/tagmaster-ios-after-phone-dark-search-keyboard.png`
- `.impeccable/review/tagmaster-ios-after-phone-dark-search.png`
- `.impeccable/review/tagmaster-ios-after-phone-dark-settings-filters.png`
- `.impeccable/review/tagmaster-ios-after-phone-dark-settings.png`
- `.impeccable/review/tagmaster-ios-after-phone-dark-share.png`
- `.impeccable/review/tagmaster-ios-after-phone-dark-tracks.png`
- `.impeccable/review/tagmaster-ios-after-phone-dark-videos.png`
- `.impeccable/review/tagmaster-ios-after-phone-light-browse-rows.png`
- `.impeccable/review/tagmaster-ios-after-phone-light-detail.png`
- `.impeccable/review/tagmaster-ios-after-phone-light-home.png`
- `.impeccable/review/tagmaster-ios-after-phone-light-rating.png`
- `.impeccable/review/tagmaster-ios-after-phone-light-search-keyboard.png`
- `.impeccable/review/tagmaster-ios-after-phone-light-search.png`
- `.impeccable/review/tagmaster-ios-after-phone-light-settings-filters.png`
- `.impeccable/review/tagmaster-ios-after-phone-light-settings.png`
- `.impeccable/review/tagmaster-ios-after-phone-light-share.png`
- `.impeccable/review/tagmaster-ios-after-phone-light-tracks.png`
- `.impeccable/review/tagmaster-ios-after-phone-light-videos.png`

## Narrow Summary correctness follow-up, 2026-09-07

This supersedes the Summary-spacing deferral in the historical progress log. Only `iOS/tagmaster/tagmaster/DPTagSummaryController.m`, existing `iOS/tagmaster/tagmasterTests/TagmasterAppLogicTests.m`, and this report changed during this follow-up. No shared-library, Android, other-screen, scheme, or configuration edits. No commits. No new screenshot exports or visual-review rounds.

### Measurement trace and fix

`DPTabBarController` removes and reattaches the selected page. `setUpRootView:withScroller:` constrains content width to the scroll viewport, not content height to viewport height. Summary uses automatic grid rows with children pinned to row edges. `DPLabel` removes intrinsic width for multiline text; its vertical hugging/compression priorities remain 250/750. Reattachment retained multiline heights measured at a transient width. Raising hugging would not correct that measurement.

Before production edits, the Lost fixture's lyrics grew from 64.3pt to 350.3pt after a page round trip, while width-constrained text still needed only 64.3pt. Notes grew from 20.3pt to 86.3pt. Scroll content grew from 351.0pt to 751.3pt. The stretched, vertically centered label accounts for the known blank region.

Summary now sets body labels' `preferredMaxLayoutWidth` to their resolved width after layout, skipping detached optional rows. The local configured key button had the same stale intrinsic-height cache, confirmed by comparing intrinsic and width-constrained fitting sizes. Its intrinsic height now uses UIKit's `sizeThatFits:` at actual width and invalidates when width changes. No fixed text heights, font reductions, grid changes, or priority overrides were added. Title, alternate title, parts, rating, type, classic number, key, sheet music, lyrics, notes and existing hide/show behavior remain intact.

### Acceptance ledger and exact frames

The two new `TMSummaryLayoutRegressionTests` run the real navigation/tab/scroll hierarchy without catalog requests. Each fixture runs at 393pt and 834pt widths on the phone simulator, initially, after two Details/Summary round trips, and after removing/restoring optional rows. The normal fixture uses Lost's title, alternate title and lyrics with short test notes. The long AX5 fixture adds wrapping titles, classic number, repeated lyrics and long notes.

| Fixture / viewport width | Lyrics height | Notes height | Scroll content before/after both round trips |
| --- | ---: | ---: | ---: |
| Lost, large / 393pt | 64.3pt | 20.3pt | 351.0 / 351.0pt |
| Lost, large / 834pt | 64.3pt | 20.3pt | 351.0 / 351.0pt |
| Long AX5 / 393pt | 3163.3pt | 2295.3pt | 6873.0 / 6873.0pt |
| Long AX5 / 834pt | 931.3pt | 435.3pt | 2084.0 / 2084.0pt |

- Pass: content labels match width-constrained text heights within 1pt; key row matches fitted title plus existing padding/minimum target. The long AX5 scroll height comes from actual wrapped text, not empty row growth.
- Pass: lyrics and notes text start beside their headings, measured offset 0.0pt in every final probe, with a 2pt assertion bound.
- Pass: first and last lines of lyrics and notes scroll into the viewport after both page round trips. Reachability allows 0.5pt for pixel rounding.
- Pass: optional rows disappear and return, content shrinks while hidden, and restored content height matches the initial height within 1pt.
- Pass: semantic Body font and Dynamic Type adjustment verified at large and AX5.

### Final test results

Using the workspace, scheme, destination and `/tmp/tm-dd` flags documented above:

```sh
xcodebuild -workspace iOS.xcworkspace -scheme tagmaster -configuration Debug \
  -destination 'platform=iOS Simulator,id=C8B74E44-94F7-4CED-A47F-DFF98E34237B' \
  -derivedDataPath /tmp/tm-dd -parallel-testing-enabled NO \
  -only-testing:tagmasterTests/TMSummaryLayoutRegressionTests \
  -only-testing:tagmasterTests/TMPolishRegressionTests \
  -resultBundlePath /tmp/tm-summary-final-unit.xcresult test
```

- Final unit run: **14 tests, 0 failures**, including 2 Summary tests and all 12 existing polish regressions. Log `/tmp/tm-summary-final-unit.log` includes the numeric frame probes.
- Existing `tagmasterUITests/TagMasterPolishUITests/testSummaryLyricsRemainReachableAfterChangingPages`: **1 test, 0 failures on each actual device class**, phone and iPad, at `accessibility-extra-extra-extra-large`. Ran `test-without-building` with that single selection. Logs `/tmp/tm-summary-final-phone-ax5.log` and `/tmp/tm-summary-final-ipad-ax5.log`; matching `.xcresult` bundles. No capture method invoked.
- Test builds compiled the production Summary controller, confirmed by `CompileC` in `/tmp/tm-summary-resolved.log`. No redundant standalone production build.
- Existing test source registration confirmed in `project.pbxproj`; no new registration needed. LSP discovery remained unavailable; compiler and XCTest supplied validation.
- Local layout detector returned `[]`. Final source diff whitespace check passed. Shared libraries and Pitch Perfect remain unchanged. `DPTagPageControllerBase.m` matches its pre-task SHA.

### Failures observed while reaching the final result

Initial test compilation rejected `selectedIndex` because this app uses `DPTabBarController`, not `UITabBarController`. The fixture now uses the real tab delegate. `/tmp/tm-summary-baseline-2.log` then reproduced 68 failing assertions across the two tests before production changes. The first width fix resolved body spacing but exposed key-button cached heights; subsequent frame probes isolated and fixed that cache. Probe corrections accounted for the rating value wrapping above its bar and floating-point scroll-edge rounding. All final assertions pass; none were disabled. A trailing test-file blank line found by `git diff --check` was removed.

Both simulators were recorded light/large before this follow-up. AX5 UI runs restored those settings in EXIT traps; final `simctl ui` queries confirmed light/large on both. No other simulator settings were changed. Fixture windows restore the previous key window and do not change persistent text settings.
