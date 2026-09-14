# iOS tag loading and full-width pages

## Status and scope

Completed on iOS Browse and tag Detail. Both use four equal, labelled bottom slots spanning their entire safe available width. The floating UITabBar sizing limitation is resolved with an app-local native-button page switcher. There is no width-test exemption remaining.

Changes are confined to `iOS/tagmaster/`, this report, and four requested reduced JPEGs. Android's completed equal-width slots are untouched. No shared depolllib, Pitch Perfect, Home/navigation/account/sync persistence, build configuration, plist, signing, commits or pushes changed. The Xcode project change only registers the new local header/source.

## Acceptance ledger

- Initial pending fetch hides unpopulated pages, page controls and actions, including accessibility descendants. Back remains available.
- Cached success mounts content immediately. Delayed success replaces the loader without waiting for animation.
- Error stops motion and offers current-ID Retry. Refresh preserves content and the chosen page on success and failure.
- Requests retain immutable IDs/generations. Stale completions and obsolete retry closures cannot replace another tag; busy counts balance even after release.
- Reduce Motion, background, disappearance, hidden and detached states stop the quartet animation.
- Browse order remains Latest, Rating, Downloads, Classic, initially Latest. Detail remains Summary, Details, Tracks, Videos, initially Summary.
- Each visible button occupies one quarter of the controller's safe width. No maximum width, narrow centered group or outside gutter is imposed.
- Native labels wrap through AX5. Targets stay at least 44pt. Selection, grouped accessibility and safe-area/sidebar clearance are tested.
- Existing page selection calls, child containment, navigation return, refresh, metadata, green checks and blue selection remain functional.

## Preserved quartet loading work

`DPTagViewController.m` keeps the completed loading implementation. Only obsolete tab-sizing constraints and the inherited root-view declaration were removed for the new local base.

- A compact five-line staff with four decorative blue notes accompanies "Gathering the quartet…" and the current tag ID. The initial loader is opaque and suppresses watermark/placeholder content. Refresh keeps the original small progress indicator.
- Loading status is one combined VoiceOver element. One screen-change announcement is issued per request generation, not one per note. Synchronous cache completion never announces loading.
- Nil/error recovery uses inline Retry for the current request. No production delay, forced animation finish, progress percentage, imaginary stage, sound or haptics was added.
- Share/actions remain disabled and absent until content exists. Back and native interactive-pop remain available.
- Note animation resumes only when initial loading is pending, attached, visible and active. Motion stops on Reduce Motion, failure, success, background and detachment.
- Loading status contrast still passes the existing light/dark >=4.5:1 assertions. Dynamic Type, scroll reachability and optional content remain covered.
- The KVO correction still clears only an actually loaded old tag, avoiding a redundant initial nil notification.

The original visibility regression failed before implementation. Original loading logs remain in `/tmp/tm-loading-*.log`; they are historical evidence, not the final width result.

## Full-width implementation

`TMPageViewController.h/.m` contains the app-local page container and `TMPageSwitcher`. Browse and Detail share it. It uses four real `UIButton` controls with `UIButtonConfiguration` inside an equal-distribution `UIStackView`, pinned directly to the controller's safe-area leading, trailing and bottom anchors. Height follows the tallest wrapped native caption, with a 56pt minimum.

The switcher keeps the existing UITabBarItem titles, SF Symbols and item order. The legacy `tabBar.items`, `selectedItem` and delegate selection calls act on the real visible controls. There is no hidden UITabBar, duplicate accessibility control, private UIKit subview manipulation, compatibility plist or global appearance workaround.

Only the selected page is attached. Page changes issue containment and appearance transitions, preserve retained page instances, and do nothing on reselection. Tests check actual parent transitions and exact appearance counts, including push/pop and removal. UIKit 26 can repeat a didMove notification during first appearance; the test distinguishes that notification from an actual reattachment.

`DPTagQueryViewController.m` recognizes the local parent so Browse keeps its inherited background and content insets. Correct containment exposed a cached first-layout text height in Summary. The local `TMSummaryBodyLabel` measures at its actual column width and invalidates on width changes. The existing summary row-height, optional-row and page-round-trip assertions remain unchanged and pass. No shared layout library was edited.

## Native bounds and behavioral evidence

The width tests measure `tabBar.buttons`, the actual visible controls. For slot i, they assert x equals safe.x + i × safe.width / 4 and width equals safe.width / 4, within half a point for pixel rounding. They also assert the bottom edge, label fit, font scale, hit testing, selected traits and parent/view identity.

Representative attached-window measurements, applicable to both Browse and Detail:

| Configuration | Safe horizontal interval | Four slot widths |
| --- | --- | --- |
| Phone portrait | 0–402pt | 100.5pt each, pixel-rounded |
| Phone landscape-sized window | 0–874pt | 218.5pt each, pixel-rounded |
| iPad portrait, full width | 0–834pt | 208.5pt each |
| iPad split detail, portrait | 330–834pt | 126pt each |
| iPad split detail, landscape-sized window | 330–1210pt | 220pt each |
| iPad narrow window | 0–600pt | 150pt each |

The matrix runs normal and AX5 fonts, portrait/landscape-sized native windows and split-column transitions. Sidebar clearance is checked against its real converted bounds. UIKit readiness uses event predicates, not sleeps or disabled animation.

Separate XCUITests rotate the actual phone and iPad, tap all eight Browse/Detail page controls, assert equal visible widths and hittability, and retain the selected page across rotation. The iPad journey exercises the real app's sidebar Browse column and secondary tag detail, not only a test-window reconstruction.

## Test results

Built from `iOS/iOS.xcworkspace`, scheme `tagmaster`, derived data `/tmp/tm-dd`. New source/header and existing test Sources registrations were mechanically checked. LSP tools were unavailable; native compilation and XCTest supplied validation.

On each iOS 26.5 simulator, 34 focused tests are verified across the final suite and isolated corrected lifecycle rerun:

- 9 existing deterministic quartet-loading tests.
- 2 visible full-width/adaptive selection tests and 1 child-lifecycle test.
- 15 existing polish/error/share/accessibility regressions.
- 2 unchanged summary-layout regressions.
- 5 existing app-logic tests, including loaded-tag/KVO, child refresh and random-tag navigation.

The final suite runs each had 33 passing tests and a lifecycle-fixture readiness failure. Only that failed test was rerun after waiting for actual destination appearance before popping; it passes on both devices. Earlier stale summary metrics and custom button-layout attempts were corrected, not waived. All equal-width/span assertions now pass normally.

Three real-app XCUITests also pass on each device: full-width page taps/rotation, polish screens/share dismissal, and summary lyrics after changing pages. No skips or width exemptions. The full repository suites and physical-device VoiceOver/performance checks were not run. Legacy test-window teardown still emits appearance warnings outside the new loading/width fixture.

Final evidence outside the repository:

- `/tmp/tm-width-phone-verified.log` and `/tmp/tm-width-ipad-final.log`: focused suites, including passing loading, width, summary and impacted tests.
- `/tmp/tm-width-phone-lifecycle-verified.log` and `/tmp/tm-width-ipad-lifecycle-final.log`: corrected lifecycle test, 1/1 on each.
- `/tmp/tm-width-ui-phone.log` and `/tmp/tm-width-ui-ipad.log`: native journeys, 3/3 on each.
- `/tmp/tm-width-ui-{phone,ipad}.xcresult`: native screenshot/test attachments.

## Screenshots and restoration

Four final JPEGs in `.impeccable/review/` come from XCUIScreen captures of the running app with loaded catalog content:

- `tagmaster-ios-fullwidth-phone-browse.jpg`
- `tagmaster-ios-fullwidth-phone-detail.jpg`
- `tagmaster-ios-fullwidth-ipad-browse.jpg`
- `tagmaster-ios-fullwidth-ipad-detail.jpg`

Phone images are 368×800; iPad images are 552×800. All four were inspected individually. They show full safe-width slots, retained metadata/green checks, charcoal bottom chrome and blue selection. The iPad detail capture visibly clears the sidebar. Earlier `tagmaster-ios-loading-*` images remain as loading-history captures; these four supersede their loaded tab geometry.

Both user app containers were backed up before testing to `/tmp/tm-width-backup` and restored after all tests/captures. Checksum dry-run comparisons reported zero differences for both. Phone C8B74E44-94F7-4CED-A47F-DFF98E34237B remains dark/large; iPad 55A9555A-9714-4FE0-8F0D-1035652526F9 remains light/large. Tests restore device orientation and use disposable-window font overrides. Apps were stopped after restoration. No raw logs were added to the repository. `git diff --check` passes.

## Final coordinator review

Native pending-state captures were inspected on Android and iPhone/iPad; the new full-width iOS Browse/Detail captures were inspected on phone and iPad. The active iOS caption initially used system blue, only 3.26:1 against charcoal. It now uses the established bright blue #5AC8FA with a controlled 14% selected fill. Contrast is 4.76:1 over the selected fill, 6.28:1 over charcoal. A regression checks every selected item in both appearances. The four full-width iOS captures precede this contrast-only correction; geometry is unchanged.

Final targeted run `/tmp/tm-loading-review-final.log`: 4/4 passed on iPhone, covering the new contrast check, equal full-width controls, page containment/appearance, and delayed pending-to-loaded behavior. This brings distinct focused iOS unit coverage to 35 across the recorded runs, with no expected failures. No unrelated full suite was rerun. Source registration and CRLF-aware whitespace checks passed. Shared libraries and protected account/signing files remain unchanged.
