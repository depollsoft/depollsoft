# Tag Master iOS round 2

Fable improvement pass, 2026-09-07. iOS only, uncommitted, in the audit worktree. Android, `.github/`, secrets and signing untouched. Shared `iOS/depolllib` unchanged.

## Scores

The original audit scored the pre-fix app. No round-1 score table was written, so the round-1 column below is reconstructed from `verification-ios.md` (what round 1 shipped and what it deferred). The round-2 column is this pass's own assessment against the audit's five dimensions after the captures below.

| # | Dimension | Audit | Round 1 (reconstructed) | Round 2 | Why it moved in round 2 |
| --- | --- | ---: | ---: | ---: | --- |
| 1 | Accessibility | 1 | 3 | 4 | Dynamic Type everywhere including handwriting title metrics; native menus replace segments at accessibility sizes (now with a menu chevron); every bar button, row, filter and track has a name, trait or hint; Details values wrap at large sizes. Hardware VoiceOver still not exercised. |
| 2 | Performance | 2 | 2 | 3 | `URLSession` with a 15 s timeout replaces `dataWithContentsOfURL`; learning tracks stream through `AVPlayer` and cache afterwards; no full-screen scrim anywhere. Thumbnail and catalog fetches still go through the same synchronous helper on background queues. |
| 3 | Appearance and theming | 2 | 3 | 4 | Semantic colours only; grouped page colour behind inset groups; SF Symbols for tabs, rows and empty states; adaptive launch screen; 21 legacy PNGs removed. The barber-pole watermark repeats in both iPad columns. |
| 4 | Platform conformance | 2 | 2 | 4 | Large handwriting title collapsing inline; `UISearchController` in the bar; inset-grouped Settings and Search; inline loading and `UIContentUnavailableConfiguration` states; disclosure chevrons; Quick Look with its bar visible; `tagmaster://tag/N` deep link. In-screen page tabs on Browse and Tag detail remain (a deliberate round-1 decision). |
| 5 | Adaptivity | 1 | 2 | 3 | `UISplitViewController` on iPad with a placeholder column and correct collapse; readable content width; page tabs pinned to the safe area so the floating sidebar no longer hides the first tab. Landscape and multitasking widths not captured. |
| | **Total** | **8/20** | **12/20** | **18/20** | |

## What changed in round 2

All production edits are under `iOS/tagmaster/tagmaster/`.

### Loading, networking and recovery

- `DPRemoteLocation` fetches through `URLSession` with a 15 s timeout; `DPTag.rate` and the XML parser use the same helper over HTTPS.
- `TMBusyIndicator` (`DPTagPageControllerBase`) counts in-flight work and reports changes; Tag detail shows a navigation-bar spinner and a loading/unavailable content configuration instead of a full-screen scrim; Random Tag shows progress on its own row; Summary buttons show their own activity; tracks stream (`DPTagTracksController.swift`).
- Search results show a centred symbol, message and Retry for empty and failed queries (`DPTagQueryViewController`).

### Navigation and structure

- iPad: double-column split view with a placeholder detail, `showTagWithId:from:` routing, collapse keeps a chosen tag on top (`DPAppDelegate`). Page tabs pinned to the safe area on iPad (`DPTagViewController`).
- Home: large handwriting title that collapses inline, no synthetic Main header, Edit disabled when there are no favourites, footer links as tinted link buttons, favourites empty-state footer (`DPHomeViewController`).
- Search: `UISearchController` in the bar plus an inset-grouped options list; Settings rebuilt as an inset-grouped list with value labels and grouped page colour (`DPSearchViewController`, `DPSettingsController`).
- Sheet music: Quick Look is pushed, so Back and the key-note button are visible from the first frame (`DPTagSummaryController`).
- Deep link accepts `tagmaster://tag/N` and `tagmaster:///tag/N` in addition to the original `tagmaster://open/tag/N`.

### Type, layout and iconography

- `TMWrappingButton` (`DPTagPageControllerBase`) measures wrapped titles at the real width; used by the Summary key button and the Details link, poster, arranger and singer values. Details headings keep a single-line width so the heading column is sized from them; names without a website render as plain text, not disabled controls; dates use locale styles.
- Page content sits on 16 pt margins inside the readable width; Summary alternate title aligned and secondary.
- Video rows: headline for who sang it, secondary subheadline metadata; track rows carry a play symbol and a hint; tag rows carry disclosure chevrons (`DPTagVideoController`, `DPTagTracksController`, `DPTagCell`).
- SF Symbol page tabs on Browse and Tag detail; `SFSafariViewController` for videos; adaptive launch screen; placeholder strings removed from `tagmaster-Info.plist`; legacy PNG tab images removed from the project.

### Tests

- `TagmasterAppLogicTests.m` and `TagDetailUITests.swift` extended in round 1 and kept green; two unit tests were adjusted earlier in round 2 for the rating button configuration and the search table layout.

## Verification

Run from `iOS/` against iPhone 17 Pro (`C8B74E44-94F7-4CED-A47F-DFF98E34237B`, iOS 26.5).

```sh
xcodebuild -workspace iOS.xcworkspace -scheme tagmaster -configuration Debug \
  -destination 'platform=iOS Simulator,id=C8B74E44-94F7-4CED-A47F-DFF98E34237B' \
  -derivedDataPath /tmp/tm-dd build > /tmp/tm-r2-build.log 2>&1
# exit 0, ** BUILD SUCCEEDED **

xcodebuild ... -parallel-testing-enabled NO -only-testing:tagmasterTests test > /tmp/tm-r2-unit.log 2>&1
# exit 0, Executed 167 tests, with 0 failures, ** TEST SUCCEEDED **

xcodebuild ... -parallel-testing-enabled NO -only-testing:tagmasterUITests test > /tmp/tm-r2-ui.log 2>&1
# exit 0, Executed 39 tests, with 0 failures, ** TEST SUCCEEDED **

git -c core.whitespace=cr-at-eol diff --check -- iOS
# clean
```

Both test runs were taken on the final source: the UI run right after the last build, and the unit run repeated afterwards because its first pass predated the last three layout edits. The `pitchperfect` scheme was not built because `depolllib` is unchanged.

## Screenshots

All under `.impeccable/review/`, captured with `xcrun simctl io <UDID> screenshot` on the iPhone 17 Pro and iPad Pro 11-inch (M5) simulators from the final build, driven by `idb ui tap` and the `tagmaster://tag/1809` deep link. Light captures use the default `large` content size; `-large` files use `accessibility-large`; `-tablet` files are the iPad in portrait.

`tagmaster-ios-round2-` + `home`, `home-dark`, `home-large`, `home-tablet`, `home-tablet-dark`, `browse`, `browse-dark`, `browse-tablet`, `search`, `search-dark`, `search-keyboard`, `search-large`, `search-tablet`, `search-results`, `summary`, `summary-dark`, `summary-large`, `summary-tablet`, `summary-tablet-dark`, `summary-after-ql`, `details`, `details-dark`, `details-large`, `tracks`, `tracks-dark`, `tracks-large`, `tracks-tablet`, `videos`, `videos-dark`, `videos-large`, `videos-tablet`, `sheetmusic`, `sheetmusic-dark`, `rating`, `rating-dark`, `settings`, `settings-dark`, `settings-large`, `settings-tablet`, `settings-tablet-dark`, `teachable`, `teachable-dark`.

`search-keyboard` shows no software keyboard because the simulator has a hardware keyboard attached; that setting was not changed. `rating`, `rating-dark`, `teachable`, `teachable-dark`, `home-large` and `settings-large` were captured on the 17:36 build; none of the later edits touch those screens.

## Inspection rounds

One full inspection round found ten defects, fixed in one batch. One confirmation round found two regressions from that batch (Details heading column collapsed once the value buttons yielded width; a trait override that did nothing) and one still-open item (iPad page tabs). A final targeted pass fixed those three with the wrapping button, single-line headings and the safe-area constraint, confirmed by the captures listed above.

## Deferrals

- **In-screen page tabs.** Browse and Tag detail still use a bottom `UITabBar` for sibling pages, per the round-1 decision to keep the shared container. On iPad the bar now stays inside the visible column. Moving to a segmented control or top tabs is a larger navigation change.
- **Watermark in both iPad columns.** The barber-pole background renders in the sidebar and the detail column. Keeping it only in the detail column needs a split-aware background, and the audit's must-not-change list protects the watermark.
- **Duplicate Search action on the Search screen.** The bar's magnifier runs the search with filters only; it duplicates the keyboard's Search key but `DPSearchViewControllerTests` and `TagmasterAppLogicTests` assert the bar item exists with the `Search` label, so it stays.
- **Landscape, multitasking widths, hardware VoiceOver, audible playback, live sign-in and sync.** Not exercised; portrait simulator evidence only.
- **Universal Links and associated domains.** Only the custom scheme is handled; no association file was deployed.
- **Thumbnails and catalog queries** still use the synchronous helper on background queues; only the timeout and the track streaming changed.

## Device restoration

Both simulators were light / `large` before this pass and were queried as light / `large` after it. Temporary capture scripts and reduced images under `/tmp` were removed; the three verification logs remain at `/tmp/tm-r2-build.log`, `/tmp/tm-r2-unit.log` and `/tmp/tm-r2-ui.log`.
