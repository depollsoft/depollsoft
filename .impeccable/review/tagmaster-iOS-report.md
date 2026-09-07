# Tag Master iOS implementation report

## Scope and status

Continued the existing refresh in `iOS/tagmaster/`; did not restart it or change Pitch Perfect/shared libraries. The first command verified the exact assigned worktree. Seed `a858fb14`, candidate 6, singing desk direction retained. An Astra-only read-only subagent reviewed controller paths. Main owns the fresh finish review and final design documentation.

## Acceptance ledger

- [x] Native Home, Browse, Search destinations, Settings utility, handwriting identity and charcoal/blue semantic theme.
- [x] Home Find a tag, Random Tag, Open Tag ID, Teachable Tags, Favorites, editing and attribution links retained.
- [x] Browse Latest/Top Rated/Downloads/Classic with remembered selection; Search filters and results retained.
- [x] Summary/Details/Tracks/Videos order; native share, rating, list actions, sheet music, pitch and media entry points retained.
- [x] iPad persistent native tabs in narrow/portrait windows; tiled sidebar in wide windows. Detail splits when content width is at least 760pt and text is below accessibility categories. Narrow windows and accessibility sizes use one readable pane.
- [x] Preferred weighted system descriptors scale exactly once. Handwriting alone uses UIFontMetrics. Native top-level large headings; detail stays inline. Selectors have 44pt minimum heights and become readable native menu buttons at accessibility sizes.
- [x] Empty, loading, first-page error/retry, later-page retry and list-empty states implemented. Save and selection haptics retained.
- [x] Storage/sync source and migrations unchanged. Deep links tested through the new root from Search to Home. Public detail selectors and Xcode source registrations checked.
- [x] Targeted hard assertions and real native phone/tablet captures, including dark/accessibility XXXL and landscape.
- [ ] Actual signed-in sync/account operations and physical-device media/haptic checks. No accounts were supplied.
- [ ] Fresh independent finish review, owned by Main.

## Screen inventory

| Path under `iOS/tagmaster/tagmaster/` | Implemented/verified coverage |
| --- | --- |
| `TMRootController.swift`, `DPAppDelegate.m` | Three independent navigation stacks; explicit Find action resets Search to its form; deep-link Home focus and dismissal of an existing presentation before pushing. Narrow tablet tabs avoid a blocking sidebar overlay. |
| `TMTheme.swift` | Semantic colors, native weighted type, retained wordmark and quiet identity, state view, adaptive field rows and accessible native choice menus. Opening contract retains finish-review-pending marker. |
| `DPHomeViewController.m/.swift`, `DPTagCell.m` | Desk actions, inline favorites, removal/reordering, described rows/materials, utility Settings, footer attribution/terms/donation. Native Home heading replaces the redundant section heading. |
| `DPBrowseViewController.m` | Four native collection choices; child replacement instead of nested tab bars; remembered `browse.collection` selection. |
| `DPSearchViewController.m` | Search field, keyboard submission, filters, query action, persisted search settings. Accessibility label is on the real text field. |
| `DPTagQueryViewController.m` | Paginated results, loading, empty/error recovery. A later-page failure preserves loaded rows and offers an explicit retry. |
| `DPTeachableTagsController.m/.swift` | List editing/reordering and empty state. Data notifications now update both empty state and Edit availability. |
| `DPTagViewController.m` | Summary/Details/Tracks/Videos; adaptive split containment; refresh, list menu, anchored sharing. Menu labels refresh on data changes; handlers follow the displayed add/remove intent. |
| `DPTagPageControllerBase.m` | Scrolling readable-width content. Width preference outranks label hugging so narrow summary panes do not collapse around their contents. |
| `DPTagSummaryController.m` | Title, key, chart, rating, parts/type/classic reference, lyrics and notes. Rating popover has a source anchor. Chart pitch handles touch cancellation; pitch stops when interrupted or leaving the summary. |
| `DPTagDetailController.m` | Posted/arranged/sung attribution, dates, downloads, tag ID, refresh time and source link in adaptive rows. |
| `DPTagTracksController.m/.swift` | Learning tracks and notes, loading failures, native AVKit presentation. Player pauses on dismissal and app interruption; late downloads cannot present from an offscreen controller. |
| `DPTagVideoController.m` | Teaching/performance videos, thumbnails, performer/key/date metadata, empty state and external YouTube opening. No new inline video playback engine. |
| `DPSettingsController.m/.swift` | Account/sign-in entry, random filters, list clearing, account deletion entry, app utilities and attribution; native labels/controls and confirmation anchors. |

`DPAppDelegate.swift`, including `depollsoft.pitchperfect.lists`, `favorite`, `teachable`, migration from the old arrays and Firestore fields, has no diff. Search/random settings keys and catalog/cache implementation remain intact. UI tests use the real app and live/cached catalog data, including tag 1, Smile. They do not substitute a mocked home screen.

## Verification commands and results

All xcodebuild commands ran from `iOS/` with full output redirected to the listed `/tmp/*.log` before filtering. Shell programs used `set -o pipefail`; test/build timeout was 1200 seconds, cached test-only runs 600 seconds. Derived data and SPM cache: `/tmp/tm-dd`.

Common build/test arguments:

```sh
xcodebuild -workspace iOS.xcworkspace -scheme tagmaster -configuration Debug \
  -destination 'platform=iOS Simulator,id=DEVICE_ID' \
  -derivedDataPath /tmp/tm-dd -parallel-testing-enabled NO \
  -resultBundlePath /tmp/RESULT.xcresult TEST_SELECTION test
```

Devices:

- iPhone 17 Pro, iOS 26.5, `C8B74E44-94F7-4CED-A47F-DFF98E34237B`.
- iPad 11-inch simulator, iOS 26.5, `55A9555A-9714-4FE0-8F0D-1035652526F9`.

| Log/result basename in `/tmp/` | Selection and result |
| --- | --- |
| `tm-ios-unit-continuation` | `-only-testing:tagmasterTests`: 169 passed, 2 failed out of 171. Failures were in new tests: Firebase was not configured in the unit-test host for URL handling, and the font regression exposed incorrect trait/scaling mechanics. Both were inspected and fixed. The prior worker's exit 137 was cancellation, not an app failure. |
| `tm-ios-targeted` | New unit/UI classes. Found the search-field accessibility-label mismatch. Its three failing phone UI flows were subsequently corrected and rerun. |
| `tm-ios-phone-confirm` | Weighted-font regression plus Find action and native screen inventory: 3 tests passed. |
| `tm-ios-tablet` | First tablet probes found sidebar occlusion and a sidebar cell/button lookup mismatch. The native layout and lookup were corrected. |
| `tm-ios-tablet-confirm` | All 21 then-current `TMRefreshTests` passed. Seven of eight tablet UI tests passed; inventory reached the real share popover but incorrectly queried Copy as a button. It is a native cell. Changed assertion to the activity-list container and reran. |
| `tm-ios-final-behavior` | Two new media/pagination tests and portrait/landscape inventory with actual sharing: 4 tests passed. |
| `tm-ios-phone-final-capture` | Final phone inventory and sharing, `test-without-building`: passed. |
| `tm-ios-tablet-dark-final` | Dark/accessibility XXXL Home and Search with actual native menu selection to Rating: passed. |
| `tm-ios-phone-dark-final` | Same dark/large-text behavioral probe on phone: passed. |
| `tm-ios-pane-confirm` | Added summary-pane width assertion plus portrait and landscape inventories: 3 tests passed. Build succeeded. No further UI changes after this run; only contract comments and this report. |

All 23 new unit tests have passing results across the targeted runs. All nine phone UI flows and ten tablet UI flows have passing results across the recorded runs. This is accumulated targeted evidence, not a claim that the whole legacy suite was rerun unchanged after every fix. Existing legacy tests sometimes swallow exceptions; the new tests use hard XCTest assertions.

The scheme now includes the existing UI-test target. `TMTheme.swift`, `TMRootController.swift`, `TMRefreshTests.swift` and `TMDeskUITests.swift` are registered in their Xcode source phases. `git diff --check -- iOS/tagmaster` passed. Swift edit diagnostics were clean. Main identified stale CocoaPods paths in auxiliary ObjC clang diagnostics; the actual SPM-backed Xcode build is the compile authority.

## Native captures

Saved under `.impeccable/review/`:

- `phone-iOS.png`, `tablet-iOS.png`.
- `phone-{search,browse,summary,details,tracks,videos,share}-iOS.png`.
- `tablet-{search,browse,summary,details,tracks,videos,share}-iOS.png`.
- `tablet-landscape-{home,search,browse,summary,details,tracks,videos,share}-iOS.png`.
- `phone-home-dark-large-iOS.png`, `phone-search-dark-large-iOS.png`.
- `tablet-home-dark-large-iOS.png`, `tablet-search-dark-large-iOS.png`.

Captures came from Simulator/XCTest, exported with `xcrun xcresulttool export attachments`. Landscape files use the full native display capture and a 90-degree orientation normalization with `sips`; no UI was composited or retouched. The final pane confirmation replaced the tablet portrait/landscape files. Dark/large-text captures predate only the final readable-width priority change, not the typography/menu/contrast fixes. Appearance was explicitly set using `xcrun simctl ui DEVICE_ID appearance dark`; accessibility XXXL used the app launch argument `-UIPreferredContentSizeCategoryName UICTContentSizeCategoryAccessibilityXXXL`.

## Limitations

- No signed-in accounts, so cloud sync, provider login, account deletion and cross-device synchronization are not claimed as verified. Their existing implementation and keys were preserved; local state/notification behavior was tested.
- Native capture and UI assertions prove simulator layout and routing, not hardware audio latency, speaker output, haptics, VoiceOver gestures or sustained performance.
- Live rating submission, external YouTube playback and every remote chart/track download were not exercised. Media dismissal/interruption behavior has direct unit assertions.
- Narrow regular-width behavior has a deterministic 600pt layout test; portrait and landscape iPad windows were captured. Freely dragged Stage Manager windows and every OS/version combination were not manually exercised.
- Final design critique and documentation remain with Main. No commits were created.
