# iOS SwiftUI

Pitch Perfect and Tag Master draw every screen in SwiftUI. This page records how
the UIKit screens were ported, the conventions the SwiftUI code follows, and how
to check a screen against its reference captures.

## Shape of a screen

- **One view, one model.** A screen is a `View` (`SongListScreen`,
  `TagDetailScreen`) plus, when it has behaviour, an `@Observable @MainActor final
  class` model (`SongListModel`). The model owns state and exposes intents
  (`addSong()`, `select(listId:)`, `toggleFavorite()`); the view only lays out and
  forwards gestures. Anything worth testing lives in the model.
- **Dependencies are injected with production defaults.** Models take the stores
  and services they use (`DPSongsModel.sharedInstance`, a tag loader, a clock) as
  initializer parameters with defaults, so tests can pass fakes without swizzling.
- **Stores stay the source of truth.** The Objective-C and Swift model layers
  (`DPSongsModel`, `DPSettingsModel`, `TMTagLists`, `DPTag`) are not rewritten.
  Screens observe them through the models, which translate their notifications
  into observable state.
- **Presentation state is model state.** Sheets, alerts, confirmation dialogs and
  navigation paths are driven by optional/enum properties on the model, not by
  view-local `@State`, so a test can open and answer them.

## Pixel parity

Each UIKit screen was captured before it was ported and the SwiftUI screen was
diffed against that capture, as the Android Compose port did.

- `iOS/shared/ScreenCatalog.swift` renders a full-screen window on the host scene
  (real safe areas and Liquid Glass bars) and writes `<name>.png` when
  `SCREEN_CATALOG_DIR` is set. Pass it to a test run as
  `TEST_RUNNER_SCREEN_CATALOG_DIR=/path xcodebuild test ...`.
- Each app's `*ScreenCatalogTests` puts every screen in every interesting state
  (empty, populated, editing, errors, alerts, light and dark, iPad where the
  layout differs).
- `scripts/ios/snapshot_diff.py <reference> <candidate> --out <diffs>` reports
  every changed pixel with no tolerance and writes red-marked diff images.
- Match UIKit's metrics exactly: the same `UIFont`s bridged with `Font(uiFont)`,
  the same SF Symbol configuration (17 pt, regular, medium scale for bar
  buttons), explicit paddings taken from the old constraints, hairlines drawn at
  the same widths. Animated regions (breathing cells, the barber pole, the
  quartet) are compared at rest.
- Keep the simulator clean. A system dialog left on it (an "Open in …?" prompt
  from `simctl openurl`) keeps the host app inactive, and UIKit then never
  finishes presenting alerts; `ScreenCatalog.capture` now fails in that state.
  Preferences written with `simctl spawn … defaults write <bundle id>` land in
  a device-wide domain the app also reads, so a later in-app reset cannot clear
  them; don't seed app defaults that way.
- Run XCUITests with `xcodebuild test` after changing them: a
  `test-without-building` run can use a previously installed UI test runner.

## Driving screens in tests

`iOS/shared/SwiftUITestDriver.swift` (`UIDriver`) walks the accessibility tree of a
mounted window and activates elements by identifier or label, so hosted tests go
through the real buttons, toggles, toolbar items and links. It turns on
accessibility automation for the test process, which SwiftUI needs before it
builds that tree. Prefer, in order:

1. model tests for logic and state transitions;
2. `UIDriver` tests for wiring: that the control exists, says the right thing and
   calls the right intent;
3. screen-catalog captures for appearance.

Accessibility identifiers and labels carried over from UIKit unchanged, because
the XCUITest bundles and `scripts/release/capture.py` use them.

## Pitch Perfect

- **Entry.** `PitchPerfectMain` (`DPAppDelegate.swift`) starts the SwiftUI
  `PitchPerfectApp`, or a bare `DPTestAppDelegate` host under XCTest. The Swift
  `DPAppDelegate` keeps the launch work (Firebase, consent, audio session, the
  JSON aliases the stores serialize with, widget playback) through
  `@UIApplicationDelegateAdaptor`. Auth callbacks arrive through the scene's
  `onOpenURL`, and Privacy choices is offered whenever the scene becomes active.
- **Shell.** `PitchPerfectRoot` is a `TabView` of four `NavigationStack`s. The
  tabs' models live in `PitchPerfectModels`, made once per app (or per test), so
  a tab comes back as it was left and tests can reach its state.
- **Screens.** `PitchPipeScreen`, `NotesScreen` and `KeysScreen`
  (`InstrumentScreens.swift`), `SongListScreen` with `SetListSelector`,
  `SetListsScreen`, `SongEditorScreen`, `AddSongsScreen`, `SettingsScreen`,
  `LoginIntroScreen`, and the shared `PrivacyChoicesView`
  (`iOS/shared/TelemetryConsent.swift`, also used by Tag Master through
  `TelemetryConsent.present(from:)`).
- **Style.** `PlateStyle.swift` bridges DPTheme's colours and fonts,
  `StaffBackground` tiles the etched staff from the same origin the UIKit
  pattern colours used (a screen's staff starts below the bars; a full-screen
  table's starts at the top), and `BarSymbol` draws bar symbols at UIKit's bar
  configuration. `plateList()` and `plateRow()` reproduce the UITableView rows:
  rules inset 20 pt, 1 pt rows added for separators, rules that stay put in edit
  mode.
- **Sound.** `NotePlayer` sounds every note and makes playing observable.
  Notes, Keys and Songs rows press through `TouchPressSurface`, a transparent
  `UIView` with the UIKit cells' touch semantics: a press lasts until that
  finger lifts, wherever it slides, and a scroll cancels it (SwiftUI's
  `isPressed` ends when the finger leaves the row and begins again when it
  returns, stopping a note early or toggling it twice). A VoiceOver
  activation toggles with Toggle Notes and otherwise sounds the note for
  1.5 s, as the pitch pipe's cells do.
- **Songs.** Rows reach the `List` as values (`SongRowData`: title and key),
  because a `List` reloads a row only when its element changes, and the song
  is one object across edits. A List also drops a row update made while a
  presented editor is going away, so the rows are redrawn once it has gone.
  Each list keeps its exact scroll offset: the rows report the List's scroll
  view and the model files and restores `contentOffset` on every switch of
  list, wherever it came from (the selector, the Set Lists screen, another
  device), clamped as UIKit did. SwiftUI's `ScrollPosition` does not drive a
  `List`. The selector reveals the chosen position the same way whenever the
  positions change size, since `ScrollViewReader` cannot reach views placed by
  a custom `Layout`.
- **Pitch pipe.** `InstrumentGeometry` and `PitchPipeModel` hold the layout and
  touch rules; `InstrumentRenderer` draws the face into a `Canvas` with the same
  Core Graphics calls the UIKit view made. `MultiTouchSurface`, a transparent
  `UIView`, is the one UIKit view left: SwiftUI gestures follow a single touch
  before iOS 18 (`SpatialEventGesture`), and chords need every finger.
- **Remaining UIKit.** The set-list naming and delete prompts are
  `UIAlertController`s presented by `SetListAlertPresenter`: SwiftUI's `.alert`
  fixes its message and buttons once shown, which would let an invalid name
  through. The song editor's content is SwiftUI inside the UIKit shell the
  editor always had (`SongEditorPresenter`: a `UINavigationController` asking
  for 320 × 480 with Close and Done bar items), because a sheet can neither
  flip over (editing an existing song) nor size itself as a form before
  iOS 18, and a `NavigationStack` in a UIKit-presented controller hands its
  title and toolbar to the presenting screen's bar. The banner is the ad SDK's
  `BannerView` in `BannerAdSlot`, which presents its overlay from its own
  screen's controller. Privacy choices, presented from UIKit, reports its
  dismissal from `PrivacyChoicesHost.viewDidDisappear`, once it has really
  gone, so the ad-consent flow that follows can present.
- **Models built once.** Screens whose model observes notifications or starts
  an auth listener keep it in `@StateObject ModelBox`: `@State`'s initial value
  is evaluated again every time the view struct is recreated.

Deliberate differences from the UIKit screens:

- Keys opens centred on C. UIKit scrolled before the table had a size, so it
  opened a row and a half from the top instead.
- Add songs' section headers show the engraved label once; UIKit also drew
  the system header title over it.
- Wake Lock keeps the screen awake. UIKit stored the setting but never
  applied it.
- Cancelling sign-in from Settings stops the Log in row's spinner.
- After Done with a blank title, the hosted SwiftUI editor's title field takes
  focus and the keyboard shortens the key list in the capture; the UIKit
  capture of the same moment showed the full list.
- The pitch pipe breathes on the clock (one breath every four seconds) rather
  than per frame, so ProMotion displays no longer breathe twice as fast.
- Toggle Notes and Wake Lock survive a relaunch. The Swift settings model
  (2021) reset both to off whenever it was created; the Objective-C model
  before it and Android always kept them.
- A toggled Songs row stays lit while its note sounds, including after the
  list redraws; UIKit's lit state was the cell's highlight and was lost on a
  reload. Pressed Notes and Keys rows show no highlight, exactly as UIKit's
  clear cells showed none.
- VoiceOver activation of a Notes, Keys or Songs row sounds the note for 1.5 s
  (or toggles it). UIKit's synthesized tap started and stopped it in the same
  instant.
- A deleted account is signed out even if Settings was closed while the
  server was deleting it; the delete confirmation reads the account's name
  when asked, not on every redraw, and never crashes on an account without an
  email.
- Keys and Settings declared portrait-only orientation masks that UIKit never
  consulted (a plain navigation controller and a page sheet do not ask), so
  they were not carried over; nothing changes for users.
- The row, rule and bar metrics match to the pixel on iPhone; the system
  controls SwiftUI draws itself (List's delete and reorder controls, glass bar
  shadows) sit within a few pixels of UIKit's, and the login explanation's
  glyph spacing differs slightly from UIKit's HTML typesetting.

## Project files

The Xcode projects list files explicitly. Register or remove files with
`iOS/<app>/tools/add_source.py` (`--target tests`, `--shared` for `iOS/shared`,
`--remove`). Object ids are hash-derived, so branches that add different files
merge without id collisions.

## Tag Master lists

Home, Browse, Search, the results list, Settings, Teachable Tags and the
user's own lists are SwiftUI (`TMHomeScreen`, `TMBrowseScreen`,
`TMQueryScreen`, `TMSearchScreen`, `TMSettingsScreen`, `TMTagListScreen`),
each over an `@Observable` model.

- **Navigation goes through `TMNavigator`.** Models ask it to open a tag, push a
  destination (`TMDestination`), remove their own screen or open a URL. The app
  supplies a `TMRouteNavigator` per screen (see *Tag Master shell*), and tests
  pass `RecordingNavigator`. `TMScreens` puts each screen and its bar together.
- **Catalog access is injected.** `TMCatalog` runs queries (tests pass
  `TMFixtureCatalog`), and `TMTagStore` loads rows on demand.
  `DPTagQueryResult.failed` separates "no matches" from "catalog unreachable".
- **UIKit metrics are copied deliberately.** `tmLabelMetrics` lays text out on
  UILabel's line-height grid. Rows use UITableViewCell's 20pt margins and
  section-aware separator insets. Filters keep `UISegmentedControl` because it
  sizes segments to their titles, and fall back to a menu when a row is narrow
  or the text size is an accessibility size.
- **Alerts.** The list-naming alert stays a `UIAlertController`
  (`TMListNamePrompt`, presented by `TMAlertPresenter`) because its message has
  to follow the typing. Every other alert is a SwiftUI `.alert`.
- **Watermark.** `TMScreenBackground` draws the barber pole on phones and
  stays clear beside the iPad split's single shared watermark.
- Captures: `TagListsScreenCatalogTests` (iPhone `iphone-*`, iPad `ipad-*`).

## Tag Master tag detail

- **Structure.** `TagDetailScreen` (TagDetailView.swift) over `TagDetailModel`, with
  `TagSummaryModel` and `TagTracksModel` for the two pages that do work. The pages are
  `TagSummaryPage`, `TagDetailsPage`, `TagTracksPage` (with the inline `TMTrackPlayer`)
  and `TagVideosPage`; `TMListChips`, `TMListPicker`, `TMSheetMusicScreen` and
  `TMTagPlaceholder` complete it. Tags come through `TMTagLoading` (production:
  `TMCatalogTagLoader`), so tests finish loads in any order without swizzling.
- **In the shell.** `TMTagRoute` carries the screen on a stack or in the split's
  detail column: it adds the backdrop, the horizontal-safe-area reading that keeps
  the page bar clear of the floating list, and the undo manager shake-to-undo
  reaches. The router sets `navigator` and `expanded`; the screen sets `screenVisible`.
- **Shared artwork.** `TMQuartetStaff(animating:)` and `TMBarberPole(compact:darkSurface:animating:)`
  (TMArtworkViews.swift) draw the vector artwork and hold still under Reduce Motion,
  in an inactive scene, or when told to. `TMBarberPole.listLoading()` and
  `.operation(_:active:)` carry the old views' accessibility labels and identifiers.
- **UIKit left in place, deliberately.** QuickLook (`TMQuickLookPreview`), the in-app
  browser (`TMSafariView`), the tag actions sheet (`TMActionSheet`; on iOS 26
  SwiftUI's `confirmationDialog` draws an anchored bubble without Cancel, not the
  centred sheet the app had) and the list picker's presentation (`TMListPickerPresenter`;
  SwiftUI's popover inside a toolbar lands on the button without an arrow).
- **Presenting from a toolbar.** Presenters in `.toolbar` content must be
  `UIViewRepresentable`s (`TMPresentingAnchor`): a view *controller* representable there
  is adopted by the navigation controller as a pushed screen. SwiftUI also builds toolbar
  items twice, once off screen, so only the copy in a window presents, once it has its
  final size. `TMPageTabBarBridge` gives the TabView's bar the
  `page-tab-bar`/`page-<Title>` identifiers and keeps it a bottom bar on iPad.
- **Parity notes.** Bar symbols are the same `UIImage` a bar button item gets, offset
  by its alignment insets. `TMFollowsUIKitTint` reads the live UIKit tint so accent
  colours dim behind sheets as UIKit's did. Beside a list on iPad the TabView sits one
  pixel inside the column's safe area; flush, it grows into the unsafe strip under the
  floating list. That pixel is the remaining iPad difference.

## Tag Master shell

- **Entry.** `TagMasterMain` (`TMShell.swift`) starts the SwiftUI `TagMasterApp`,
  or a bare `TMTestAppDelegate` host under XCTest. `DPAppDelegate` keeps the
  launch work (Firebase, consent, the cache serializers, list sync) through
  `@UIApplicationDelegateAdaptor`. Opened URLs go to the sign-in providers first
  (`+[DPAppDelegate handleAuthURL:]`) and otherwise to the router; Privacy
  choices is offered whenever the scene becomes active.
- **Layout.** iPhone is one `NavigationStack` (`TMStackRoot`) with Home at its
  root. iPad is a `NavigationSplitView` (`TMSplitRoot`): Home's stack in a
  320–400 pt column (36% of the width), the tag or `TMTagPlaceholder` beside it,
  one watermark behind both columns (screens inside the split leave their own
  backdrop clear through `tmSharedWatermark` and `tmClearColumnBackground`), and
  a chosen tag kept on top when the split collapses. The collapsed column is
  stored (`preferredCompactColumn`), decided at collapse and then following
  Back, as UIKit's top-column choice applied only at the moment of collapse.
  The columns can only be made clear from iOS 18, so on iOS 17 every screen
  keeps its own watermark (`TMSplitRoot.columnsCanBeClear`).
- **Routing.** `TMRouter` owns the list stack, the detail column's stack (the
  sheet music reader) and the tag beside the list, which it reuses from tag to
  tag so the open page survives. `show(_:)` makes each screen's model with its
  route, so a screen's `TMRouteNavigator` can remove exactly that screen and the
  tags it opens step through it (`TMListingSource`). Deep links
  (`tagmaster://tag/<id>`, `tagmaster:///open/tag/<id>`) keep the Objective-C
  delegate's exact grammar in `TMDeepLink`, and a linked tag steps through the
  list on top of the list stack (`topListingSource`), as the delegate adopted
  it. Each Browse page is its own list, so a tag keeps stepping through the page
  it was opened from.
- **Keyboard.** ⌘↑ / ⌘↓ are keyboard shortcuts on the detail's stepper buttons,
  so they reach the detail from either column. SwiftUI keeps them registered on
  the scene's root while the sheet music reader covers the detail, so they
  still step (and close the reader) there.
- **The bar.** Each screen's bar is SwiftUI toolbar content (`TMScreens`), with
  `TMEditButton` for Edit/Done. `tmCharcoalBar` reaches the UIKit navigation
  controller SwiftUI draws with to give it the charcoal appearance, set back
  titles and, on Home, the Wickhop title (`TMHomeTitle`); SwiftUI has no
  modifiers for those.
- **Tests.** `TMShellTestSupport.swift` hosts screens on a plain UIKit stack
  (`TMHostedScreen`, a test-only `TagDetailViewController`) and mounts the real
  shell (`mountShell`). `TMRouterTests` covers routing and the deep-link
  grammar; the catalogs capture through the real shell.
- **Sidebar backgrounds.** Search and Settings colour their hosting
  controller's view (`TMPageColorHook`), beneath the sidebar's glass, as the
  UIKit screens coloured their own view; painted in SwiftUI the grey sat above
  the glass and whole columns differed.
- **Bar items.** Edit's Done is UIKit's done-style checkmark in its own glass,
  and the list menu gets a glass of its own. Symbols UIKit created without a
  configuration use the large scale (`TMBarButton.symbol(_:scale:)`).
- **Page bars.** `TMPageTabBarBridge` (detail and Browse) gives the bar its
  identifiers, keeps it compact-width on iPad, and pins its vertical size class
  to regular so a phone in landscape keeps the full-height bar and 44-point
  targets the UIKit bar had. The landscape pill spans more of the width than
  UIKit's did.
- **Search.** The field keeps the bar and its Search action while in use
  (`searchPresentationToolbarBehavior(.avoidHidingContent)`), as the UIKit
  search controller did.
- **Text size.** List text is set with `tmFont` / `tmLabelMetrics` and the
  facts layout with the environment's `DynamicTypeSize`, so a text size change
  (or a window's trait override in tests) re-renders it, as UILabel's
  `adjustsFontForContentSizeCategory` did.
- **Rows.** `TMTagStore` asks again for a tag that failed to load after 30
  seconds, when the app becomes active and when the lists change, as the UIKit
  cell fetched on every configure. A list naming a tag twice keys each row by
  tag and occurrence (`TMListedTag`); catalog pages drop tags already shown.
- **Deliberate differences, from the behaviour review.**
  - Stepping to another tag rebuilds the pages, so each page starts at its top;
    the selected page is kept. UIKit kept each scroll view's offset, which would
    need the pages to go on showing the previous tag while the next loads.
  - The detail counts as visible from the start of its appearance transition
    (`onAppear`) to the end of its disappearance (`onDisappear`), not from
    `viewDidAppear` to `viewWillDisappear`: a track keeps playing through a pop
    transition, and a back swipe the user abandons no longer stops it.
  - "Rated" survives a refresh of the same tag; UIKit reset it to "Rate".
  - Before iOS 18 a list change arriving mid-scroll applies at once;
    `onScrollPhaseChange` (iOS 18) is what holds it until the scroll ends.
  - Swiping to delete a list closes the swipe before the confirmation, rather
    than holding it half-open under it.
- **Remaining differences from UIKit.** The detail catalog's iPad goldens were
  recaptured with the real Home in the list column (the first ones used an
  empty stand-in). In the iPad sidebar, tag rows' media marks sit about 1.7 pt
  higher than UIKit's (about 4% of a Browse capture).

