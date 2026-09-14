# Tag Master (iOS) — Native Audit

Date: 2026-09-07
App: `iOS/tagmaster/` (UIKit, mixed Objective-C and Swift, fully programmatic layout built on the shared `iOS/depolllib` grid; iOS 17.0 deployment target; iPhone + iPad; Mac Catalyst enabled)
Evidence devices: iPhone 17 Pro simulator `C8B74E44-94F7-4CED-A47F-DFF98E34237B` and iPad Pro 11-inch (M5) simulator `55A9555A-9714-4FE0-8F0D-1035652526F9`, both iOS 26.5, Debug build from `/tmp/tm-dd`. Screenshots reused from the earlier pass in `.impeccable/review/` (iPhone light, dark, and a larger Dynamic Type size; iPad light/dark) plus three new iPad captures made during this pass. Simulator evidence only; gestures and frame pacing were not measured on hardware. Playbook: `~/.claude/skills/impeccable/reference/audit.native.md` scored against `ios.md`. Product truth: `PRODUCT.md` (Tag Master section). `DESIGN.md` was not used because it covers Pitch Perfect only and says Tag Master is a different world.

Method note: this is a source audit. Every finding was verified in code. Screenshot evidence was checked by pixel sampling (Pillow) rather than by viewing images. The iPad crash was reproduced by attaching `lldb` to the running simulator app and invoking the share action; the simulator log captured the exception. No source files were modified. Simulator settings were left as found (light appearance on both devices; the iPhone was already at the default `large` content size).

## Audit Health Score

| # | Dimension | Score | Key Finding |
| --- | ----------- | ------- | ------------- |
| 1 | Accessibility | 1 | No `accessibilityLabel` anywhere in the app; every text size is a fixed point size so Dynamic Type does nothing (0.3% of pixels change between default and enlarged captures of Browse and Settings); list rows read as five fragments with unlabeled availability icons; pitch buttons cannot be sounded with VoiceOver |
| 2 | Performance | 2 | All networking is synchronous `dataWithContentsOfURL` with no timeout or cancellation behind a blocking overlay; learning tracks download in full before playback; favorites row heights read the disk cache on the main thread |
| 3 | Appearance & Theming | 2 | System label/background colors carry most screens, but the loading overlay, rating bar, video thumbnails, key button border, launch screen, and Holo-green check PNGs are hard-coded; the rating bar track is invisible in Dark Mode |
| 4 | Platform Conformance | 2 | Bottom tab bar (custom container) used for sibling views inside pushed screens, blocking modal spinner on every fetch, settings and search built as free-form grids, no large titles, share links do not reopen the app, deep-link scheme only works with a throwaway host segment |
| 5 | Adaptivity | 1 | Share crashes the app on iPad (unanchored popover); iPad renders the phone layout stretched to 834 pt; search results lock to portrait while every other screen rotates |
| **Total** | | **8/20** | **Poor (major overhaul)** |

## Platform Conformance Verdict

**Fail, narrowly.** This is not a ported website: it is a 2013-era UIKit app that still uses the platform's own controls almost everywhere, and a fluent iPhone user will recognize the navigation bar, action sheets, alerts, swipe-to-delete, Quick Look, and the share sheet. What breaks trust:

- **Tab bar as in-screen tabs, in a hand-rolled container.** Browse (`Latest / Rating / Downloads / Classic`) and Tag detail (`Summary / Details / Tracks / Videos`) are each pushed onto the navigation stack and then show their own `UITabBar` at the bottom, driven by `DPTabBarController`, a custom container that swaps child views by hand. The home screen has no tab bar at all. HIG reserves the tab bar for top-level sections; sibling views of one pushed screen belong in a segmented control or page control. (`iOS/tagmaster/tagmaster/DPBrowseViewController.m:34-55`, `DPTagViewController.m:101-131`, `iOS/depolllib/depolllib/DPTabBarController.m:28-105`)
- **Blocking full-screen spinner on every fetch.** `DPBusyIndicator` is pinned over the whole navigation controller view, including the navigation bar and status bar, with a hard-coded dark scrim and a `Loading...` label, and there is no cancel or timeout. Opening a tag, choosing a random tag, opening sheet music, submitting a rating, and playing a track all block the entire app. (`iOS/depolllib/depolllib/DPBusyIndicator.m:22-49,84-88`; hosts listed under P1; `tagmaster-ios-loading-dark.png` shows the scrim covering the status-bar band: mean luminance 39 vs 7 on the un-dimmed dark home capture)
- **Free-form grids where the platform expects lists.** Settings and Search are `DPGridLayout` forms with 12 pt bold header labels and segmented controls; Settings should be an inset-grouped list. (`DPSettingsController.m:52-133`, `DPSearchViewController.m:39-93`)
- **No large titles, no disclosure indicators.** Home uses a fixed 20 pt handwriting label as `titleView`; no screen sets `prefersLargeTitles`; the `Browse / Teachable Tags / Random Tag / Open Tag / Settings` rows have no accessory. (`DPHomeViewController.m:43,86-91,248-257`)
- **Links do not round-trip.** The share sheet sends `http://tags.depoll.com/tag.php?id=N`, which cannot reopen the app because there is no associated-domains entitlement; the only handled form is the custom scheme, and even that requires a throwaway host segment (`tagmaster://open/tag/1809` works, `tagmaster://tag/1809` is ignored). (`DPAppDelegate.m:106-117`, `Barbershop/DPTag.m:367-369`, `tagmaster.entitlements`)
- **iPad is a stretched phone.** One full-width navigation stack, list rows and forms span 834 pt, and the share sheet crashes because the popover has no anchor. (`DPTagViewController.m:208-213`; `tagmaster-ios-ipad-home.png`, `tagmaster-ios-ipad-tag-summary.png`)

What is native and right: `UINavigationController` with the interactive pop gesture untouched, SF Symbols in the navigation bar, `UIAlertController` alerts and action sheets, `UIActivityViewController`, `QLPreviewController` for sheet music, `AVPlayerViewController` for tracks, `UIRefreshControl` plus infinite scroll, native table editing (swipe-to-delete and drag reorder) for Favorites and Teachable Tags, a `pageSheet` FirebaseUI sign-in with Sign in with Apple, safe-area constraints on every root view, and system label/background colors by default so Dark Mode largely works.

## Executive Summary

- Audit Health Score: **8/20** (Poor)
- Total issues: **P0: 1, P1: 7, P2: 10, P3: 4**
- Top critical issues:
  1. **Share crashes the app on iPad.** `UIActivityViewController` is presented with no popover anchor; reproduced on the iPad Pro simulator (`NSGenericException: UIPopoverPresentationController ... should have a non-nil sourceView or barButtonItem`). (`DPTagViewController.m:208-213`)
  2. **Failures are silent.** A tag that fails to load pops the screen with no message (this is what `Open Tag` shows for an unknown ID), `Random Tag` does nothing on failure, and the loading overlay has no cancel and no timeout. (`DPTagViewController.m:63-86`, `DPHomeViewController.m:157-215`)
  3. **Accessibility is at floor level.** Zero accessibility labels, no Dynamic Type, availability icons that VoiceOver skips and that are both green, pitch buttons that VoiceOver cannot sound.
  4. **Navigation model is off-platform.** Custom tab-bar container for sibling views inside pushed screens, blocking spinner, grid forms instead of lists, no large titles.
  5. **Links and iPad.** Share links never reopen the app; the custom scheme needs a dummy host; iPad gets the phone layout at full width.
- Recommended next steps: fix the share anchor and the silent-failure paths first (`/impeccable harden`), then Dynamic Type (`/impeccable typeset`), accessibility labels and row grouping (`/impeccable harden`), navigation and controls (`/impeccable shape`), iPad and orientation (`/impeccable adapt`), theming (`/impeccable colorize`), networking and playback (`/impeccable optimize`), and `/impeccable polish` last.

## Detailed Findings by Severity

### P0 — Blocking

**[P0] Share crashes the app on iPad**

- **Location**: `iOS/tagmaster/tagmaster/DPTagViewController.m:208-213` (`sendTag`), invoked from the `square.and.arrow.up` bar button created at `:133-136`. Evidence: `tagmaster-ios-ipad-tag-1809.png` (tag `Lost` open on iPad) then `tagmaster-ios-ipad-share.png` (home-screen wallpaper: the app is gone); simulator log line `*** Terminating app due to uncaught exception 'NSGenericException', reason: 'UIPopoverPresentationController (<UIPopoverPresentationController: 0x115ebc000>) should have a non-nil sourceView or barButtonItem set before the presentation occurs.'`
- **Category**: Adaptivity / Conformance
- **Impact**: On any iPad in a regular-width layout, tapping Share on a tag terminates the app. Sharing a tag with the group is a primary social action in `PRODUCT.md`. The other two popovers in the app are anchored correctly (`showActions` uses `barButtonItem` at `:172`; `rate` uses `sourceView` at `DPTagSummaryController.m:215`, verified alive on iPad in `tagmaster-ios-ipad-rate-sheet.png`), so this is the only crash of its kind.
- **Guideline**: HIG Popovers / UIKit: a popover presentation must have a source view or bar button item; on iPad `UIActivityViewController` always presents as a popover.
- **Recommendation**: Keep a reference to the share bar button (as is already done for `actionBarButton`) and set `activityController.popoverPresentationController.barButtonItem` before presenting. Keep the activity items (`"<title> - Tag Master for iOS"` string plus `tag.tagUri`) unchanged.
- **Suggested command**: `/impeccable harden`

### P1 — Major

**[P1] Tag load, Open Tag, and Random Tag fail silently**

- **Location**: `DPTagViewController.m:63-86` (`loadTag:` pops the controller with no message when the tag is nil or an exception is raised); `DPHomeViewController.swift:31-52` (`Open Tag` alert pushes `DPTagViewController` for any integer, so an unknown ID shows the loading scrim and then bounces back to Home); `DPHomeViewController.m:157-215` (`Random Tag`: two synchronous queries, every failure branch only decrements the busy count); `DPTagSummaryController.m:257-284` (sheet music download failure is swallowed; `writeData:` is called with nil data if the fetch fails); `DPTagTracksController.swift:31-35` (track download failure swallowed).
- **Category**: Conformance (feedback) / Accessibility
- **Impact**: With no network, or with a mistyped tag ID, the user sees a full-screen `Loading...` scrim and is then returned to where they were, with no explanation and no retry. During an afterglow this reads as "the app is broken".
- **Guideline**: HIG Feedback: communicate status and offer a way forward; errors should say what happened and what to do.
- **Recommendation**: Route every failure branch through one helper that presents a `UIAlertController` (or an inline error view on the detail screen) with a plain message (`Couldn't load tag 1809. Check your connection and try again.`) and a `Retry` action; for `Open Tag`, keep the alert open and show the error in place when the ID is unknown. Do not change the query parameters or cache behavior.
- **Suggested command**: `/impeccable harden`

**[P1] A blocking full-screen `Loading...` scrim covers the whole app on every fetch, with no cancel or timeout**

- **Location**: `iOS/depolllib/depolllib/DPBusyIndicator.m:22-49` (overlay: `colorWithWhite:0.2 alpha:0.8`, white spinner, `lightTextColor` label) and `:84-88` (disables interaction). Hosts: `DPHomeViewController.m:111-125` and `DPTagViewController.m:149-162` (pinned edge-to-edge over `navigationController.view`, so the navigation bar and Back are covered), `DPHomeViewController.m:158` (random tag), `DPTagViewController.m:64` (tag load), `DPTagSummaryController.m:240,258` (rating, sheet music), `DPTagTracksController.swift:16` (track), `DPSettingsController.m:139-148`. Every fetch is a synchronous `dataWithContentsOfURL:` with the system default timeout and no cancellation: `Barbershop/DPTagXMLParser.m:215-221`, `DPTagSummaryController.m:263`, `DPTagTracksController.swift:20`, `DPTag.m:389`. Evidence: `tagmaster-ios-loading-dark.png` (status-bar band mean 39 vs 7 in the undimmed dark capture: the scrim covers the bar).
- **Category**: Conformance
- **Impact**: The user cannot go back, switch tabs, or do anything until the request returns; on a flaky venue connection a stalled request leaves the app frozen for up to a minute with nothing to tap. The scrim is also hard-coded dark on both appearances.
- **Guideline**: HIG Loading: show progress without blocking; keep navigation available; prefer inline `UIActivityIndicatorView` / `UIProgressView` or `UIRefreshControl`.
- **Recommendation**: Replace the overlay with per-screen inline state: a `UIActivityIndicatorView` in the navigation bar (`UIBarButtonItem(customView:)`) or a centered indicator inside the content area only, leaving the navigation bar interactive. Disable only the initiating control (the tapped row, the `Rate` button, the track row). Move networking to `URLSession` with a 15 s timeout so a stalled request ends in the P1 error path above. Keep `DPBusyIndicator` for `DPTagCell` (where it dims only the row) if desired.
- **Suggested command**: `/impeccable shape`

**[P1] Dynamic Type is ignored everywhere; row heights are frozen at first use**

- **Location**: `DPTagPageControllerBase.m:42-61` (`boldSystemFontOfSize:12`, `systemFontOfSize:12`, `boldSystemFontOfSize:24` used by Summary, Details, Tracks, Videos, Search, Settings); `DPTagCell.m:54-67` (10 pt, 15 pt, 12 pt); `DPTagSummaryController.m:110,122` (18 pt, 12 pt); `DPHomeViewController.m:43,65-77` (20 pt handwriting title, `smallSystemFontSize` footer); `DPBusyIndicator.m:40-44`. No call to `preferredFontForTextStyle:`, `UIFontMetrics`, or `adjustsFontForContentSizeCategory` exists in the target. Cell heights are computed once in `dispatch_once` from a template cell (`DPTagCell.m:218-245`) and returned from `heightForRowAtIndexPath:` (`DPHomeViewController.m:313-324`, `DPTagQueryViewController.m:217-219`, `DPTeachableTagsController.m:96-99`); video rows are fixed at 68 pt (`DPTagVideoController.m:193-195`). Evidence: pixel diff between default and enlarged captures is 0.3% for Browse and Settings, 2.2% for Home (only the system-styled `Main` rows grow), 2.9% for Summary.
- **Category**: Accessibility
- **Impact**: Singers who rely on larger text (common in dim afterglow rooms and for older members) get 10 to 12 pt metadata, 12 pt lyrics, and 12 pt settings labels that never scale. `Sheet Music` and `Learning Tracks` captions are 10 pt, under the 11 pt floor.
- **Guideline**: HIG Typography: use text styles so text follows the user's size; 11 pt minimum.
- **Recommendation**: Map the three helpers to text styles (`makeHeader` → `.subheadline` semibold via `UIFontMetrics`, `makeBodyLabel` → `.body`, `makeTitleLabel` → `.title1` bold), set `adjustsFontForContentSizeCategory = YES`, and use `.footnote` for cell metadata and `.caption1` for the two availability captions. Switch the tag lists to self-sizing rows (`rowHeight = UITableViewAutomaticDimension`, `estimatedRowHeight`) and delete the cached-height statics. Keep the handwriting title for the Home display moment but derive its size from `UIFontMetrics(forTextStyle: .title2)`.
- **Suggested command**: `/impeccable typeset`

**[P1] VoiceOver: no accessibility labels in the app, list rows read as fragments, availability icons are skipped and are both green**

- **Location**: `grep -rn accessibility iOS/tagmaster/tagmaster` finds only the SwiftUI Apple button (`DPSettingsController.swift:78-81`). Tag rows: `DPTagCell.m:51-79` (five separate labels/images, the cell is not an accessibility element) and `:206-207` (`ic_check_yes.png` / `ic_check_no.png` `UIImageView`s with no label; both PNGs are Holo green, mean opaque color `#8ED92A` vs `#99CB00`, so the state is carried by shape alone). Video rows: `DPTagVideoController.m:156-162` (`Multitrack` check image). Bar buttons: `DPAppDelegate.m:203-216` creates icon-only items with no label; `DPTagViewController.m:137-140` uses the `tag` symbol for the Favorite/Teachable menu, which announces as `Tag`. Home `Edit` toggles table editing for Favorites only but sits on the left of a screen whose first section is navigation (`DPHomeViewController.m:98`). Rating action-sheet titles are star glyphs (`DPTagSummaryController.m:216-230`) that VoiceOver spells out character by character.
- **Category**: Accessibility
- **Impact**: A VoiceOver user swipes through five stops per tag row, hears `Sheet Music` and `Learning Tracks` on every row regardless of availability, cannot tell the three navigation-bar buttons apart by purpose, and hears `black star black star white star...` when rating.
- **Guideline**: HIG Accessibility / VoiceOver: label every control, group related content into one element, convey state, do not rely on color or glyph alone.
- **Recommendation**: Make `DPTagCell` a single accessibility element with a composed label (`"Lost. Also known as ... Rating 4.5, posted January 25 2011, 500 downloads. Sheet music available. Learning tracks available."`) and trait `.button`; replace the two PNGs with SF Symbols (`checkmark.circle.fill` / `circle`) tinted with the app tint and `.secondaryLabel`, plus `accessibilityLabel` on the captions. Give the navigation-bar items explicit labels (`Search`, `Share`, `Favorite and Teachable options`, `Refresh`) and hints. Title rating actions `5 stars` ... `1 star` (glyphs may stay as a suffix). Add `accessibilityLabel` to the `Key` buttons (`Play key note C`).
- **Suggested command**: `/impeccable harden`

**[P1] Tab bars used for sibling views inside pushed screens, through a custom container that does not forward appearance callbacks**

- **Location**: `DPBrowseViewController.m:34-55` (four `UITabBarItem`s with PNG icons `History.png`, `Favorites.png`, `Downloads.png`, `Bookmarks.png`), `DPTagViewController.m:101-131` (`Summary / Details / Tracks / Videos` with `TagSummary`, `MostViewed`, `Tracks`, `Videos` PNGs), `iOS/depolllib/depolllib/DPTabBarController.m:64-105` (removes and re-adds child views on selection without `beginAppearanceTransition`/`didMoveToParentViewController`; `viewControllers` setter never calls `didMoveToParentViewController:`). Only `.png` and `@2x.png` variants exist for these icons (no `@3x`), so they upscale on every modern iPhone. Evidence: `tagmaster-ios-browse.png`, `tagmaster-ios-tag-summary.png` (bar occupies the bottom band; tab-bar region mean 242 vs 255 on Home which has none).
- **Category**: Conformance
- **Impact**: A tab bar appears two levels deep and vanishes when the user goes back, which contradicts the platform's mental model (tab bar = app-level sections). Children never receive `viewWillAppear` on tab switches, so the Videos and Tracks pages cannot refresh on appearance. Legacy raster icons sit next to SF Symbols in the same navigation bar.
- **Guideline**: HIG Tab bars: top-level sections only; use a segmented control (or page control) for sibling views within a screen. HIG Icons: SF Symbols, weight-matched.
- **Recommendation**: In both screens, replace `DPTabBarController` with a `UISegmentedControl` as `navigationItem.titleView` (Browse) or below the title (Tag detail) driving a `UIPageViewController` with the same four child controllers and order. Keep the child classes, titles, and query parameters exactly as they are. Retire the eight PNG icons or convert them to SF Symbols (`clock`, `star`, `arrow.down.circle`, `book`; `doc.text`, `info.circle`, `waveform`, `play.rectangle`) if an icon is still wanted.
- **Suggested command**: `/impeccable shape`

**[P1] Links do not reopen the app; the custom scheme needs a dummy host; no universal links**

- **Location**: `DPAppDelegate.m:106-117` (`url.pathComponents.count == 3 && pathComponents[1] == "tag"`: `tagmaster://tag/1809` yields host `tag`, path `/1809`, two components, and is ignored; `tagmaster://open/tag/1809` works, verified on the iPad simulator via `simctl openurl`); `Barbershop/DPTag.m:367-369` (`tagUri` is `http://tags.depoll.com/tag.php?id=N`, used by the share sheet at `DPTagViewController.m:208-213`); `tagmaster.entitlements` has no `com.apple.developer.associated-domains`; `tagmaster-Info.plist:37-48` registers only the `tagmaster` scheme. `DPTagDetailController.m:69` links to `http://www.barbershoptags.com/dbpage.php?...`.
- **Category**: Conformance
- **Impact**: A tag shared from the app opens Safari on the recipient's phone even when they have Tag Master installed, and links from BarbershopTags.com never open the app. `PRODUCT.md` lists sharing and "open a tag by ID" as core to the social use case. The Android audit found the same gap on its side, so cross-platform links are broken in both directions.
- **Guideline**: Universal Links (associated domains + `apple-app-site-association`); handle `NSUserActivityTypeBrowsingWeb` in the app delegate.
- **Recommendation**: Add `applinks:tags.depoll.com` (first-party, can ship now) and `applinks:www.barbershoptags.com` (needs the site owner's AASA) to the entitlements; implement `application:continueUserActivity:restorationHandler:` to parse `tag.php?id=` and `dbpage.php?pg=view&dbase=tags&id=` into the existing `DPTagViewController` push; accept both `tagmaster:///tag/N` and `tagmaster://tag/N` by reading the last path component when either the host or a path component equals `tag`. Do not change the `tagUri` string or share text (the Android app and the website depend on them).
- **Suggested command**: `/impeccable harden`

**[P1] iPad renders the phone layout stretched to full width**

- **Location**: single `UINavigationController` root with no split view (`DPAppDelegate.m:78-83`); content width bound to the scroll view width with 8 pt padding (`DPTagPageControllerBase.m:72-108`); tag rows span the full content view (`DPTagCell.m:87-102`); Settings and Search grids stretch their star column across the window (`DPSettingsController.m:63-67`, `DPSearchViewController.m:49-53`). No `readableContentGuide`, `UISplitViewController`, or size-class branches anywhere. Evidence: `tagmaster-ios-ipad-home.png`, `tagmaster-ios-ipad-browse.png`, `tagmaster-ios-ipad-tag-summary.png`, `tagmaster-ios-ipad-tag-tracks.png` (1668×2420 captures of the same single-column layout).
- **Category**: Adaptivity
- **Impact**: 12 pt lyrics run 800 pt wide, list rows are mostly empty space, and every tap into a tag replaces the whole list. Teachers preparing material on an iPad (a stated audience) get the weakest experience.
- **Guideline**: HIG Layout / iPadOS: adapt to size classes; sidebar + detail for list/detail apps; readable line widths.
- **Recommendation**: Wrap the app in a `UISplitViewController` (`.doubleColumn`, `preferredDisplayMode = .oneBesideSecondary`) on regular width: Home/Browse/Search/Teachable in the primary column, tag detail in the secondary; on compact width keep the existing stack. Constrain grid forms and the Summary/Details scroll content to `readableContentGuide`. Keep the two-tab-row phone behavior untouched.
- **Suggested command**: `/impeccable adapt`

### P2 — Minor

**[P2] Key/pitch buttons cannot be sounded with VoiceOver and keep sounding on touch cancel**

- **Location**: `iOS/pitchperfect/pitchperfectlib/pitchperfectlib/DPPitchPipeButton.m:69-86` (`TouchDown` plays, `TouchUpInside|TouchUpOutside` stops; no `TouchCancel`), used on the Summary tab at `DPTagSummaryController.m:121-125`; the Quick Look `Key:` bar button at `DPTagSummaryController.m:270-274` wires the same two events. An unused `playKeyNote` that plays for 1.5 s already exists at `:294-300`.
- **Category**: Accessibility
- **Impact**: VoiceOver activation sends only the up event, so the note never sounds for screen-reader users; a system gesture or incoming call that cancels the touch leaves the note playing until the tag changes.
- **Guideline**: HIG Buttons: respond to activation; handle all touch phases.
- **Recommendation**: Add `UIControlEventTouchCancel` to the stop handler in both places; when `UIAccessibility.isVoiceOverRunning` (or via `accessibilityActivate`), call the existing 1.5 s `playKeyNote` path.
- **Suggested command**: `/impeccable harden`

**[P2] Rating flow: no confirmation, no persisted "rated" state, plain-HTTP fire-and-forget**

- **Location**: `DPTagSummaryController.m:211-255` (action sheet, `ratingButton` disabled after success but re-enabled on every `refreshView` at `:99`), `Barbershop/DPTag.m:386-390` (`http://` rating URL, result discarded, errors ignored). Evidence: `tagmaster-ios-rate-sheet.png`, `tagmaster-ios-ipad-rate-sheet.png`.
- **Category**: Conformance
- **Impact**: The user gets no acknowledgment that the rating was recorded, can rate again after any refresh, and the request rides the ATS exception for `barbershoptags.com` (`tagmaster-Info.plist:92-106`) instead of HTTPS, which the search API already uses (`DPTag.m:12`).
- **Recommendation**: Use `https://` for the rating endpoint (keep `client=TagMaster&action=rate&id=&rating=`), show a brief inline `Thanks for rating` label and keep a `rated.<id>` list in `NSUserDefaults` mirroring Android's `RatedIds`, and surface failures through the shared error helper.
- **Suggested command**: `/impeccable harden`

**[P2] Hard-coded colors and a non-adaptive launch screen; the rating bar track disappears in Dark Mode**

- **Location**: `DPBusyIndicator.m:35-44` (`colorWithWhite:0.2 alpha:0.8`, `whiteColor`, deprecated `lightTextColor`); `DPTagSummaryController.m:113` (`ratingBar.backgroundColor = colorWithWhite:0 alpha:0.1`: black on black in dark); `DPTagVideoController.m:113` (`darkGrayColor` thumbnail placeholder); `DPPitchPipeButton.m:42` (`colorWithWhite:0.5` border; fill uses `systemGray2/5`); `iOS/tagmaster/LaunchScreen.xib:14,26,39` (`calibratedWhite 0.28` background with a 320×480 `LaunchImage.png`, mean color `#555555`, shown in both appearances); `DPTagCell.m:171-187` Holo-green check PNGs. Evidence: `tagmaster-ios-tag-summary-dark.png`.
- **Category**: Theming
- **Impact**: The scrim and launch screen are the same charcoal in light and dark; the rating track has no visible extent in dark so a 2.1 rating and a 4.9 rating look like a floating blue sliver; the green Android-style check marks clash with the iOS palette.
- **Guideline**: HIG Color: semantic colors; Dark Mode first-class. HIG Launch screen: match the first screen, adapt to appearance.
- **Recommendation**: `ratingBar.trackTintColor = .tertiarySystemFill`, `progressTintColor = tintColor`; overlay scrim `.systemBackground.withAlphaComponent(0.7)` with `.label` text and a `UIBlurEffect(.systemMaterial)`; thumbnail placeholder `.secondarySystemFill`; launch screen: system background plus the app mark, no bitmap. Replace check PNGs with SF Symbols as above.
- **Suggested command**: `/impeccable colorize`

**[P2] Touch targets under 44 pt**

- **Location**: Home footer links are four rows of `smallSystemFontSize * 1.5` ≈ 19.5 pt (`DPHomeViewController.m:55-60,61-84`); the `Key` button uses 4 pt vertical content insets on a 12 pt font (`DPTagSummaryController.m:122-125`, about 28 pt tall); `Rate`, `Sheet Music`, and the Details-tab link buttons are bare system buttons around 30 pt tall (`DPTagSummaryController.m:117-128`, `DPTagDetailController.m:112-123`); the `Key:` custom bar button in Quick Look (`DPTagSummaryController.m:270-274`).
- **Category**: Accessibility
- **Impact**: Mis-taps on `Sheet Music` (the single most important control on Summary) and on attribution links.
- **Guideline**: HIG: 44×44 pt minimum with spacing.
- **Recommendation**: Give the footer a `UIStackView` of 44 pt rows; make `Sheet Music` a filled `UIButton.Configuration` button with a `doc.richtext` symbol and 44 pt height; set `minimumContentSize`/insets on `Rate`, `Key`, and the link buttons.
- **Suggested command**: `/impeccable harden`

**[P2] Settings and Search are free-form grids; Home rows lack disclosure; no large titles; search bar outside the navigation bar**

- **Location**: `DPSettingsController.m:52-133` (grid of headers, body text, three system buttons, four segmented controls, saved on `viewDidDisappear` at `:271-276`); `DPSearchViewController.m:39-93` (grid with a bare `UISearchBar`, five segmented controls, a tap gesture to dismiss the keyboard at `:102-106`, and a `magnifyingglass` button that duplicates the keyboard's Search key at `:97-100`); Home `Main` section cells with no `accessoryType` and a section header literally titled `Main` (`DPHomeViewController.m:248-257,267-277`); no `prefersLargeTitles` in the target. Evidence: `tagmaster-ios-settings.png`, `tagmaster-ios-search.png`, `tagmaster-ios-home.png`.
- **Category**: Conformance
- **Impact**: Settings does not look or navigate like iOS Settings; `Log In` / `Clear Favorites` / `Clear Teachable Tags` are floating blue text; VoiceOver users hear the segmented values without their headers (labels are separate views, not `accessibilityLabel`s). Home's navigation rows give no push affordance.
- **Guideline**: HIG Lists and tables: inset-grouped lists for settings-shaped content; disclosure indicators for navigation rows; large titles on top-level screens; `UISearchController` in the navigation bar.
- **Recommendation**: Rebuild Settings as an inset-grouped `UITableView` (`Account` row with `Log In`/`Log Out`, `Favorites`/`Teachable Tags` clear rows styled destructive, `Random Tag Filters` rows using `UIMenu` pop-up buttons or the existing segmented controls as cell accessories). Move the search field into a `UISearchController` on the Search screen and keep the options as an inset-grouped list. Add `.disclosureIndicator` to the Home navigation rows and rename `Main` to nothing (no header) or `Tag Master`. Enable large titles on Home and inline titles elsewhere.
- **Suggested command**: `/impeccable shape`

**[P2] Search results lock to portrait while every other screen rotates**

- **Location**: `DPTagQueryViewController.m:128-134` returns `UIInterfaceOrientationMaskPortrait`; `tagmaster-Info.plist:125-137` declares landscape support on iPhone and iPad; Browse embeds the same class inside `DPTabBarController` where the override has no effect, so Browse rotates and Search Results does not.
- **Category**: Adaptivity
- **Impact**: Pushing search results from a landscape Search screen snaps the interface to portrait, and on iPad in landscape the whole window rotates for one screen.
- **Guideline**: HIG Orientation: support both unless there is a reason the user would thank you for.
- **Recommendation**: Delete the two overrides.
- **Suggested command**: `/impeccable adapt`

**[P2] Teachable Tags entry disappears when the list is empty; Favorites and Teachable have no empty state**

- **Location**: `DPHomeViewController.m:147-154` (row only added when `teachable.count > 0`); `DPTeachableTagsController.m` has no empty view (the `Favorites` section on Home shows only its header when empty). Evidence: `tagmaster-ios-home.png` (no `Teachable Tags` row), `tagmaster-ios-teachable.png`.
- **Category**: Conformance (discoverability)
- **Impact**: The feature that teachers rely on cannot be found until a tag has been marked teachable from the `tag` menu on a detail screen; a user who clears the list loses the entry point.
- **Recommendation**: Always show `Teachable Tags`; add `UIContentUnavailableConfiguration` (iOS 17+) empty states: `No favorites yet. Use the tag menu on any tag to add one.` and the equivalent for teachable.
- **Suggested command**: `/impeccable clarify`

**[P2] Status and error text is an editable `UITextView` showing raw exception text**

- **Location**: `DPTagQueryViewController.m:85` (`UITextView` with default `editable = YES`, no font), `:145-151` (installed as `tableHeaderView` via `sizeToFit`), `:190-192` (`An error has occurred: <NSException reason>`), `:186-188` (`No tags could be found that matched your query.`).
- **Category**: Conformance
- **Impact**: Tapping the `No tags could be found` message opens a keyboard and lets the user edit it; network failures show internal exception text.
- **Recommendation**: Use `UIContentUnavailableConfiguration.search()` for no results and a plain `UILabel`/content-unavailable view with a `Retry` button for errors; keep the two message strings' meaning.
- **Suggested command**: `/impeccable harden`

**[P2] Main-thread disk reads in row-height callbacks; tracks download in full before playing; no `URLSession`**

- **Location**: `DPHomeViewController.m:313-324` and `DPTeachableTagsController.m:96-99` call `DPTag.loadFromCache:` per row, which reads and deserializes a property list from disk on a miss (`Barbershop/DPTag.m:188-198`, `iOS/depolllib/depolllib/filecache/DPFileCache.m:36-43`); `DPTagTracksController.swift:17-30` writes the whole MP3 to a temp file with `Data(contentsOf:)` before `AVPlayer` starts; `DPTag.m:91-158` batches ID lookups by sleeping 100 ms and polling every 10 ms; video cells are rebuilt without reuse and fetch thumbnails with `dataWithContentsOfURL` (`DPTagVideoController.m:94-190`).
- **Category**: Performance
- **Impact**: First scroll of a long Favorites list stutters while each height triggers a file read; a learning track on a slow venue connection shows the blocking scrim for the whole download instead of starting to play after buffering.
- **Recommendation**: Keep an in-memory height/`DPTag` cache warmed on a background queue before `reloadData`, or switch to self-sizing rows (see the Dynamic Type finding) so heights are not needed up front. Play tracks with `AVPlayer(url: remote)` (streams immediately) and cache with `AVAssetDownloadTask` or a `URLSession` download afterward; keep the `DPFileCache` key scheme. Move all fetches to `URLSession` with timeouts.
- **Suggested command**: `/impeccable optimize`

**[P2] Info.plist carries a placeholder photo-library purpose string and unused location strings; entitlements claim photo access**

- **Location**: `tagmaster-Info.plist:113-114` (`NSPhotoLibraryUsageDescription` = `{human-readable reason for photo access}`), `:107-112` (three location strings saying the app does not use location), `:121-124` (`UIRequiredDeviceCapabilities` `armv7`), `tagmaster.entitlements:15-16` (`personal-information.photos-library`). No code in the target touches Photos or Core Location.
- **Category**: Conformance (App Review / privacy)
- **Impact**: A placeholder purpose string is grounds for App Review rejection if any linked SDK ever requests photo access, and unused privacy strings contradict the product's privacy commitments in `PRODUCT.md`.
- **Recommendation**: Remove the photo and location usage strings and the photo entitlement (verify the Facebook SDK build does not require them), and drop `armv7`.
- **Suggested command**: `/impeccable harden`

### P3 — Polish

**[P3] Icon assets are 1x/2x PNGs only, mixed with SF Symbols**

- **Location**: `iOS/tagmaster/tagmaster/*.png` (`History`, `Favorites`, `Downloads`, `Bookmarks`, `MostViewed`, `TagSummary@2x`, `Tracks@2x`, `Videos@2x`; no `@3x`), `iOS/tagmaster/ic_check_*@2x.png`, next to SF Symbol bar buttons (`DPAppDelegate.m:203-216`).
- **Category**: Theming
- **Impact**: Slightly soft tab icons on every 3x iPhone; two icon languages on one screen.
- **Recommendation**: Covered by the SF Symbols replacement in the tab-bar and check-mark findings.
- **Suggested command**: `/impeccable colorize`

**[P3] Footer links use `http://`, the copyright year comes from the build date, and the brand is cased `Depollsoft`**

- **Location**: `DPHomeViewController.m:62-76`.
- **Category**: Conformance
- **Impact**: Extra redirect hop on each link; `Depollsoft` differs from the `DepollSoft` used in `PRODUCT.md` and the Android app.
- **Recommendation**: `https://` URLs, `DepollSoft © <current year>` from `Calendar`, keep the four link destinations and `Content provided by BarbershopTags.com` wording.
- **Suggested command**: `/impeccable clarify`

**[P3] Videos leave the app; remote-notification registration with no handler**

- **Location**: `DPTagVideoController.m:197-208` (`openURL` to `youtube.com/watch`), `DPAppDelegate.m:58` (`registerForRemoteNotifications` with no delegate callbacks; `aps-environment` in entitlements).
- **Category**: Conformance
- **Impact**: Watching a teaching video means switching apps and coming back; the push registration does nothing.
- **Recommendation**: Present videos in `SFSafariViewController` (keeps the user in the app and still hands off to the YouTube app if installed); remove the registration call unless push is planned.
- **Suggested command**: `/impeccable polish`

**[P3] Custom container skips child appearance callbacks; video and track cells are never reused**

- **Location**: `DPTabBarController.m:64-105`; `DPTagVideoController.m:178` and `DPTagTracksController.m:100` allocate a new cell per call.
- **Category**: Performance
- **Impact**: Negligible today (lists are short) but blocks per-tab refresh logic and Dynamic Type self-sizing.
- **Recommendation**: Superseded by the segmented-control/page-view replacement; register and dequeue cells.
- **Suggested command**: `/impeccable optimize`

## Patterns & Systemic Issues

- **Fixed point sizes on every label.** Three helper methods (`DPTagPageControllerBase.m:42-61`) and the cell (`DPTagCell.m:54-67`) define the whole type system as 10/12/15/18/24 pt literals; fixing the helpers fixes six screens at once, but the frozen row-height cache must go with them.
- **Zero accessibility labels.** Not one `accessibilityLabel`, `accessibilityHint`, or `isAccessibilityElement` call in the app target; every icon-only control depends on SF Symbol defaults and every composite row is fragmented.
- **One blocking spinner for every asynchronous operation.** Seven call sites increment the same full-window `DPBusyIndicator`; there is no non-blocking loading pattern anywhere.
- **Synchronous Foundation networking.** `dataWithContentsOfURL:` / `stringWithContentsOfURL:` / `Data(contentsOf:)` in four places, wrapped in `@try/@catch` blocks that discard the error; no timeouts, no cancellation, no `URLSession`.
- **Hand-rolled layout containers instead of platform lists.** `DPGridLayout` builds Settings, Search, Summary, Details, Tracks, and Videos; `DPTabBarController` replaces `UITabBarController`/`UIPageViewController`. These are the root of the conformance and adaptivity scores.
- **Popover anchors are set ad hoc.** Two of three popovers are anchored; the third crashes on iPad. A single `present(_:from:)` helper would prevent regressions.
- **Persistence keys are shared with Pitch Perfect by name.** `DPAppDelegate.swift:21` stores Tag Master lists under `depollsoft.pitchperfect.lists`; harmless but must not be "corrected" without a migration.

## Positive Findings

- Platform components where it counts: `UIAlertController` for alerts and action sheets, `UIActivityViewController` for sharing, `QLPreviewController` for sheet music (pinch-zoom, share, and print for free), `AVPlayerViewController` for tracks, `UIRefreshControl` with infinite scroll on lists.
- The interactive pop gesture is never disabled; safe-area guides are used on every root view (`DPTagPageControllerBase.m:95-107`, `DPTabBarController.m:41`, `DPTagQueryViewController.m:97-108`).
- System semantic colors by default: window and views use `systemBackgroundColor`, labels use `labelColor`, so Dark Mode already works on most screens (`DPAppDelegate.m:75,219`; dark captures show correct inversions).
- SF Symbols in the navigation bar with a consistent 17 pt regular configuration (`DPAppDelegate.m:203-216`).
- Native table editing for Favorites and Teachable Tags: swipe-to-delete, `Edit` reorder, and move-guarding so favorites cannot be dragged into the navigation section (`DPHomeViewController.m:297-345`).
- Feed dates parse correctly (`E, d MMM yyyy`, `Barbershop/DPTagXMLParser.m:22`, live feed checked: `Mon, 26 Dec 2011`), unlike the Android port.
- ID lookups for favorites are batched into one API call (`DPTag.m:91-158`), and tags are cached to disk with a version stamp (`DPTag.m:188-198,324-328`).
- Audio session category is `.playback` so tracks and key notes play over the silent switch (`DPAppDelegate.swift:95-99`).
- Sign-in uses the FirebaseUI SwiftUI picker in a `pageSheet` with Sign in with Apple, email, Google, and Facebook, and a proper nonce (`DPSettingsController.swift:35-138,200-234`).
- Firestore sync writes only the changed list with `mergeFields` and never echoes server data back (`DPAppDelegate.swift:27-38,56-67,131-132`).

## Recommended Actions

1. **[P0] `/impeccable harden`**: Anchor the share popover to its bar button (`DPTagViewController.m:208-213`); add error alerts with retry to tag load, Open Tag, Random Tag, sheet music, and track download; accept `tagmaster://tag/N`; add universal links for `tags.depoll.com`; HTTPS rating endpoint; remove placeholder purpose strings.
2. **[P1] `/impeccable harden`**: Accessibility labels for bar buttons, single-element tag rows with composed labels, SF Symbol availability marks with labels, rating action titles in words, VoiceOver-activatable key buttons with touch-cancel handling, 44 pt targets.
3. **[P1] `/impeccable typeset`**: Text styles through the three label helpers and the cell, `adjustsFontForContentSizeCategory`, self-sizing rows, retire the height cache.
4. **[P1] `/impeccable shape`**: Segmented control + page view instead of `DPTabBarController` on Browse and Tag detail; non-blocking loading; inset-grouped Settings; `UISearchController`; disclosure indicators and large title on Home; content-unavailable empty states.
5. **[P1] `/impeccable adapt`**: `UISplitViewController` on regular width, `readableContentGuide` for forms and text pages, remove the portrait lock, keyboard-aware search form.
6. **[P2] `/impeccable colorize`**: Semantic colors for the scrim, rating track, thumbnail placeholder, and key button; adaptive launch screen; SF Symbols for tab and check icons.
7. **[P2] `/impeccable optimize`**: `URLSession` with timeouts, streaming track playback, background cache reads, cell reuse.
8. **[P2] `/impeccable clarify`**: Always-visible Teachable Tags entry, empty-state copy, footer/brand copy, error message wording.
9. **`/impeccable polish`**: Final pass after the above.

> You can ask me to run these one at a time, all at once, or in any order you prefer.
>
> Re-run `/impeccable audit` after fixes to see your score improve.

## Implementation Brief

Priority order within each bundle; bundles A–F are independent and can be worked in parallel. Every bundle must keep the unit tests (`xcodebuild test -workspace iOS/iOS.xcworkspace -scheme tagmaster -destination 'platform=iOS Simulator,id=C8B74E44-94F7-4CED-A47F-DFF98E34237B'`) and the UI tests in `iOS/tagmaster/tagmasterUITests` green. The UI tests locate controls by `navigationBars.buttons["Search"]` or `buttons["magnifyingglass"]`, `buttons["Share"]` or `buttons["square.and.arrow.up"]`, `searchFields.firstMatch`, `keyboards.buttons["Search"]`, `buttons["Cancel"]`, and `tables.firstMatch`; explicit accessibility labels `Search` and `Share` on those bar buttons keep them passing. The app skips its normal launch path when `XCTestCase` is loaded (`DPAppDelegate.m:45-50`); keep that guard.

### Bundle A — Crash, errors, links (ship first; no redesign)

1. `iOS/tagmaster/tagmaster/DPTagViewController.m:133-144,208-213` — store the share `UIBarButtonItem` in a property (like `actionBarButton`) and set `activityController.popoverPresentationController.barButtonItem` before `presentViewController:`. Preserve the activity items exactly: the `"%@ - Tag Master for iOS"` string and `self.tag.tagUri`.
2. Add one error-presentation helper (e.g. a category on `UIViewController` in `DPTagPageControllerBase` or a small Swift extension) that shows a `UIAlertController` with a message and optional `Retry`. Call it from: `DPTagViewController.m:63-86` (instead of the silent pop; pop only after the user dismisses, or leave the screen with an inline error view), `DPHomeViewController.m:173-178,196-201,212-214` (Random Tag), `DPTagSummaryController.m:278-282` (sheet music; also guard `writeData:` against nil), `DPTagSummaryController.m:249-253` (rating), `DPTagTracksController.swift:31-35` (track). Keep all query parameters, `fieldList` strings, and `cache:` flags unchanged.
3. `DPAppDelegate.m:106-117` — accept `tagmaster://tag/N` (host `tag`, one path component) as well as the current three-component form; both must push `DPTagViewController` with `tagId`. Add `application:continueUserActivity:restorationHandler:` handling `NSUserActivityTypeBrowsingWeb` for `tags.depoll.com/tag.php?id=N` and `www.barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=N`. `tagmaster.entitlements` — add `com.apple.developer.associated-domains` with `applinks:tags.depoll.com` and `applinks:www.barbershoptags.com`. Release note: publish `apple-app-site-association` at `https://tags.depoll.com/.well-known/` (first-party) and request it from the BarbershopTags.com owner. Do not change `DPTag.tagUri` output (`DPTag.m:367-369`).
4. `Barbershop/DPTag.m:386-390` — `https://` in the rating URL; keep the query string. `DPTagSummaryController.m:99,239-255` — persist a rated flag per tag ID in `NSUserDefaults` (new key, e.g. `rated.<id>`), disable `Rate` when set, and show a short confirmation label.
5. `DPTagQueryViewController.m:85,145-151,186-192` — replace the `UITextView` with a non-editable label or `UIContentUnavailableConfiguration`; replace `An error has occurred: %@` with a user-facing sentence and a `Retry` action that calls `refresh`. Keep `No tags could be found that matched your query.` (the UI tests do not assert on it, but the wording is fine).
6. `tagmaster-Info.plist:107-114,121-124` — remove `NSPhotoLibraryUsageDescription`, the three location strings, and `armv7`; `tagmaster.entitlements:15-16` — remove the photo-library entitlement. Verify with a Debug build that `FBSDKCoreKit` does not require them (the Facebook build flag in `DPAppDelegate.m:14-19` compiles either way).
7. `DPPitchPipeButton.m:80-86` and `DPTagSummaryController.m:273` — add `UIControlEventTouchCancel` to the stop events.

### Bundle B — Accessibility labels and Dynamic Type

1. `DPAppDelegate.m:203-216` — add an `accessibilityLabel:` parameter to `barButtonItemWithSystemName:` and pass `Search` (`DPHomeViewController.m:100-103`, `DPSearchViewController.m:97-100`), `Share`, `Favorite and Teachable options`, `Refresh` (`DPTagViewController.m:133-144`).
2. `DPTagCell.m` — set `isAccessibilityElement = YES` on the cell and compose `accessibilityLabel` in `setTagInstance:` from title, alternative title, rating, posted date (spelled out with `NSDateFormatter` `dateStyle = .medium`), download count, and the two availability states; mark the sub-labels and images not accessible. Replace `ic_check_yes/no` with `UIImage(systemName:)` `checkmark.circle.fill` / `circle` (tint `.tintColor` / `.tertiaryLabel`) in `onImage`/`offImage` (`:171-187`) and `DPTagVideoController.m:210-226`; keep the visual meaning (filled = available).
3. `DPTagSummaryController.m:216-230` — action titles `5 stars`, `4 stars`, … (glyphs optional as suffix); `DPTagSummaryController.m:121-125,270-274` and `DPPitchPipeButton.m` — `accessibilityLabel` `Play key note <note>`, `accessibilityHint` `Plays for one and a half seconds`, and an `accessibilityActivate` override that calls the existing `playKeyNote` (`:294-300`).
4. `DPTagPageControllerBase.m:42-61` — `makeHeader`: `UIFontMetrics(forTextStyle: .subheadline)` scaled `boldSystemFont` (or `.subheadline` with semibold descriptor); `makeBodyLabel`: `preferredFont(forTextStyle: .body)`; `makeTitleLabel`: `.title1` bold; all with `adjustsFontForContentSizeCategory = YES`. `DPTagCell.m:54-67` — title `.headline`, aka/details `.footnote`, captions `.caption1`. `DPTagSummaryController.m:110,122` — aka `.title3`, key button `.body`. `DPHomeViewController.m:43,65-77` — title via `UIFontMetrics(forTextStyle: .title2).scaledFont(for: wickhop 20)`, footer `.footnote`.
5. Self-sizing rows: `DPHomeViewController.m:313-324`, `DPTagQueryViewController.m:217-219`, `DPTeachableTagsController.m:96-99` — return `UITableViewAutomaticDimension` with `estimatedRowHeight` ≈ 84 and delete `withAkaHeight`/`withoutAkaHeight`/`tagHeight:` (`DPTagCell.m:210-253`, header `DPTagCell.h:10-13`). `DPTagVideoController.m:193-195` — automatic dimension.
6. Touch targets: `DPHomeViewController.m:54-84` footer as a vertical `UIStackView` of 44 pt buttons; `DPTagSummaryController.m:117-128` `Sheet Music` and `Rate` as `UIButton.Configuration.filled()/.tinted()` with `contentInsets` giving ≥ 44 pt; `DPTagDetailController.m:112-123` link buttons ≥ 44 pt.

### Bundle C — Navigation and controls

1. Replace `DPTabBarController` usage in `DPBrowseViewController.m:32-55` and `DPTagViewController.m:101-131` with a `UISegmentedControl` (titles `Latest / Rating / Downloads / Classic` and `Summary / Details / Tracks / Videos`) driving a `UIPageViewController` (or manual child swap with proper `addChild`/`didMove` and appearance forwarding). Keep the child controller classes, order, `sortBy`/`collection` values, and `busyIndicator` wiring. `DPTagQueryViewController.m:69-71,97-108` checks `isKindOfClass:[DPTabBarController class]` to decide on background and top constraints; update those checks for the new parent. Leave `DPTabBarController` in `depolllib` for other consumers.
2. Loading: remove the window-level `DPBusyIndicator` hosts (`DPHomeViewController.m:111-129`, `DPTagViewController.m:149-166`, `DPSettingsController.m:139-148`) and show a `UIActivityIndicatorView` as a right bar button item or centered in the content area during load; disable only the initiating control. Keep `DPBusyIndicator` inside `DPTagCell` (row-level).
3. `DPSettingsController.m:42-151` — rebuild as an inset-grouped `UITableViewController` (`Account`: Log In/Log Out with the `Log in to back up and synchronize your tag lists.` footer; `Favorites`/`Teachable Tags`: destructive clear rows keeping the existing confirmation alerts at `:278-306`; `Random Tag Filters`: four rows with `UIMenu` pop-up buttons or segmented accessories). Keep the class name, the class methods `minDownloads`/`minRating`/`sheetMusic`/`learningTracks`, the `random.*` defaults keys and their registered defaults (`:162-251`), the private-build section (`:122-133`), and `refreshLoginButton` (used by `DPSettingsController.swift:208,219`).
4. `DPSearchViewController.m:36-109` — `UISearchController` in `navigationItem` (placeholder `Search`, `obscuresBackgroundDuringPresentation = NO`), options as an inset-grouped list or a `UIStackView` inside a scroll view with keyboard `contentInset` handling; keep the `search.*` defaults keys and the index-to-enum mapping in `search` (`:176-232`).
5. `DPHomeViewController.m:248-257,267-277` — `accessoryType = .disclosureIndicator` on section 0 rows; drop the `Main` header; `navigationController.navigationBar.prefersLargeTitles = YES` with `largeTitleDisplayMode = .always` on Home and `.never` on pushed screens; always include the `Teachable Tags` row (`:147-154`); add `UIContentUnavailableConfiguration` empty states for Favorites and `DPTeachableTagsController`.

### Bundle D — Theme and assets

1. `DPBusyIndicator.m:35-44` — `UIVisualEffectView(.systemMaterial)` scrim or `.systemBackground` at 0.7 alpha, spinner `.label` color, label `.label`.
2. `DPTagSummaryController.m:112-113` — `trackTintColor = .tertiarySystemFill`, `progressTintColor = nil` (tint); `DPTagVideoController.m:113` — `.secondarySystemFill`; `DPPitchPipeButton.m:42` — `.separator`.
3. `iOS/tagmaster/LaunchScreen.xib` — system background color, centered app mark from the asset catalog, no `LaunchImage.png`; delete `LaunchImage*.png` and `screenbackground*.png` if the decorative backdrop (`DPAppDelegate.m:218-240`) is retired.
4. SF Symbols for tab/segment icons and check marks (see Bundles B and C); delete the eight PNG icon pairs in `iOS/tagmaster/tagmaster/`.

### Bundle E — Adaptivity

1. `DPAppDelegate.m:78-83` — on regular horizontal size class present a `UISplitViewController(style: .doubleColumn)` with the existing `UINavigationController` (Home stack) as primary and tag detail as secondary; on compact keep the current stack. `DPHomeViewController.m:279-295`, `DPTagQueryViewController.m:204-209`, `DPTeachableTagsController.m:77-83`, `DPAppDelegate.m:110-112`, `DPHomeViewController.swift:48-52` — route tag pushes through one `showTag(id:)` helper that uses `showDetailViewController:` so both layouts work.
2. `DPTagPageControllerBase.m:72-108`, `DPSettingsController.m`, `DPSearchViewController.m` — constrain content to `readableContentGuide` horizontally.
3. `DPTagQueryViewController.m:128-134` — delete the orientation overrides.
4. `DPSearchViewController.m` — adjust the scroll view `contentInset` from `keyboardWillChangeFrameNotification` (or use `UIScrollView.keyboardDismissMode = .interactive` and `contentInsetAdjustmentBehavior`).

### Bundle F — Performance

1. `Barbershop/DPTagXMLParser.m:215-221`, `DPTagSummaryController.m:263`, `DPTagTracksController.swift:20`, `DPTag.m:389`, `DPTagVideoController.m:170` — `URLSession` with a 15 s `timeoutIntervalForRequest`; keep the synchronous-looking API (`parseWithUrl:` can wait on a semaphore off the main thread) so callers and tests (`tagmasterTests/DPTagNetworkTests.m`) keep working.
2. `DPTagTracksController.swift:13-37` — `AVPlayer(url: track.source.uri)` immediately; cache to `DPFileCache` in the background for offline reuse using the same `cacheKey`.
3. `DPHomeViewController.m:313-324`, `DPTeachableTagsController.m:96-99` — unnecessary after self-sizing rows (Bundle B); otherwise pre-warm `DPTag.loadFromCache:` for visible IDs on a background queue before `reloadData`.
4. `DPTagVideoController.m:94-190`, `DPTagTracksController.m:99-104` — register and dequeue cells.
5. `DPAppDelegate.m:58` — remove `registerForRemoteNotifications` unless push is planned.

### Must NOT change

- **Persistence keys and formats**: `depollsoft.pitchperfect.lists` dictionary with `favorite` and `teachable` integer arrays (`DPAppDelegate.swift:21,27-31,56-60`), legacy keys `favorites`/`teachable` and their migration (`:45-50,74-90`), `search.sortBy|sheetMusic|learningTracks|parts|collection` (`DPSearchViewController.m:115-153`), `random.minDownloads|minRating|sheetMusic|learningTracks` with registered defaults 2/2/1/0 (`DPSettingsController.m:162-251`), the `DPFileCache` directory `depolllib.cache` and key scheme (`DPFileCache.m:6,49-54`, `DPRemoteLocation.m:10-12`), `DPTag.cacheKeyForId` (`DPTag.m:65-67`) and `APP_VERSION` (`:11`), the `DPJsonSerializer` registrations for `NSURL`/`NSDate` (`DPAppDelegate.m:61-72`).
- **Account and sync**: FirebaseUI providers (email, Google, Facebook, Apple with nonce, optional phone) and the `Sign in to Tag Master` string (`Localizable.strings:15`); Firestore document `users/{uid}` with `lists.favorite` / `lists.teachable`, `mergeFields` writes, `doSave:false` on inbound snapshots, and the prefetch of listed tags (`DPAppDelegate.swift:101-142`); `GoogleService-Info*.plist`; URL schemes for Google and Facebook (`tagmaster-Info.plist:27-57`); `FirebaseDataCollectionDefaultEnabled = NO` and the Facebook auto-logging flags (`:60-71`).
- **Deep-link and share contracts**: the `tagmaster` URL scheme and the existing `/tag/<id>` form (only add forms, never remove); `DPTag.tagUri` → `http://tags.depoll.com/tag.php?id=`; the share text `"<title> - Tag Master for iOS"`.
- **BarbershopTags.com API**: `API_URI_STRING` with `client=TagMaster`, the query builder parameters and both `fldlist` strings (`DPTag.m:12,115-117,240-303`), the rating endpoint query, the XML parser element names and date format `E, d MMM yyyy` (`DPTagXMLParser.m:22`), the ATS exception for `barbershoptags.com` until every call is HTTPS.
- **Terminology and attribution**: tags, learning tracks, parts, All Parts / Tenor / Lead / Baritone / Bass / Other 1–4 (`DPTag.m:330-365`), favorites, teachable tags, Classic Tags, Easy Tags, `Content provided by BarbershopTags.com`, `Terms of Use`, `Donate`, `Tag Master`, the Wickhop handwriting title (`UIAppFonts`).
- **Product behavior**: random-tag filter semantics and defaults, favorites/teachable ordering and reorder/delete, `Refresh` reloading the tag from the network (`DPTagViewController.m:215-217`), Quick Look for sheet music with the key note button, key note derivation (`DPTag.m:371-384`), `.playback` audio session, the `Latest / Rating / Downloads / Classic` browse queries and the four detail pages.
- **Test hooks**: the `XCTestCase` guard in `didFinishLaunchingWithOptions` (`DPAppDelegate.m:45-50`), the `--uitesting` launch argument, the `tagmaster` scheme name and bundle identifier `depollsoft.tagmaster`, and the accessibility names the UI tests query (`Search`/`magnifyingglass`, `Share`/`square.and.arrow.up`, `Cancel`).

## Screenshots used

All under `.impeccable/review/`. iPhone captures are 1206×2622 (iPhone 17 Pro), iPad captures 1668×2420 (iPad Pro 11-inch M5), iOS 26.5 simulators. "dark" = dark appearance; "large" = the larger Dynamic Type capture from the earlier pass.

- `tagmaster-ios-home.png`, `-home-dark.png`, `-home-large.png`, `-home-favorites.png` — Home: handwriting title, `Edit` and search bar buttons, `Main` navigation rows without disclosure, `Favorites` section with tag rows, four-link footer. Used for the Dynamic Type diff (2.2%) and the missing `Teachable Tags` row.
- `tagmaster-ios-open-tag-alert.png` — `Open Tag` alert with numeric field.
- `tagmaster-ios-browse.png`, `-browse-dark.png`, `-browse-large.png`, `-browse-rating.png` — Browse with the bottom `UITabBar` (`Latest`/`Rating`); Dynamic Type diff 0.3%.
- `tagmaster-ios-search.png`, `-search-dark.png`, `-search-large.png` — Search form (bare search bar, five segmented controls); Dynamic Type diff 11.2%, driven by the system search bar only.
- `tagmaster-ios-search-results.png`, `-search-results-dark.png` — pushed results list.
- `tagmaster-ios-loading-dark.png` — full-window `Loading...` scrim covering the navigation and status bars (top band luminance 39 vs 7).
- `tagmaster-ios-tag-summary.png`, `-tag-summary-dark.png`, `-tag-summary-large.png` — Summary tab (rating bar, `Rate`, key button, `Sheet Music`, lyrics); Dynamic Type diff 2.9%; dark capture used for the rating-track contrast note.
- `tagmaster-ios-tag-details.png`, `-tag-tracks.png`, `-tag-tracks-dark.png`, `-tag-videos.png` — the other three detail tabs.
- `tagmaster-ios-tag-actions.png` — `Add Favorite` / `Mark as Teachable` action sheet.
- `tagmaster-ios-rate-sheet.png` — star-glyph rating action sheet.
- `tagmaster-ios-sheet-music.png` — `QLPreviewController` with the `Key:` bar button.
- `tagmaster-ios-settings.png`, `-settings-dark.png`, `-settings-large.png` — grid-style Settings; Dynamic Type diff 0.3%.
- `tagmaster-ios-login.png` — FirebaseUI sign-in sheet.
- `tagmaster-ios-teachable.png`, `-teachable-edit.png` — Teachable Tags list and edit mode.
- `tagmaster-ios-ipad-home.png`, `-ipad-browse.png`, `-ipad-browse-dark.png`, `-ipad-tag-summary.png`, `-ipad-tag-tracks.png` — iPad single-column stretched layouts.
- `tagmaster-ios-ipad-tag-1809.png` (new) — tag `Lost` opened on iPad via `tagmaster://open/tag/1809` immediately before the share test.
- `tagmaster-ios-ipad-share.png` (new) — iPad home-screen wallpaper captured after invoking Share: the app had terminated (exception in the simulator log).
- `tagmaster-ios-ipad-rate-sheet.png` (new) — the rating popover on iPad, anchored correctly; app alive.
