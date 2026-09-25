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
- **Sound.** `NotePlayer` sounds every note and makes playing observable;
  rows press through a `ButtonStyle` watching `isPressed`, so a note sounds for
  as long as a finger rests on it, as the UIKit cells' touches did.
- **Pitch pipe.** `InstrumentGeometry` and `PitchPipeModel` hold the layout and
  touch rules; `InstrumentRenderer` draws the face into a `Canvas` with the same
  Core Graphics calls the UIKit view made. `MultiTouchSurface`, a transparent
  `UIView`, is the one UIKit view left: SwiftUI gestures follow a single touch
  before iOS 18 (`SpatialEventGesture`), and chords need every finger.
- **Remaining UIKit.** The set-list naming and delete prompts are
  `UIAlertController`s presented by `SetListAlertPresenter`: SwiftUI's `.alert`
  fixes its message and buttons once shown, which would let an invalid name
  through. The banner is the ad SDK's `BannerView` in `BannerAdSlot`.

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
- The row, rule and bar metrics match to the pixel on iPhone; the system
  controls SwiftUI draws itself (List's delete and reorder controls, glass bar
  shadows) sit within a few pixels of UIKit's, and the login explanation's
  glyph spacing differs slightly from UIKit's HTML typesetting.

## Project files

The Xcode projects list files explicitly. Register or remove files with
`iOS/<app>/tools/add_source.py` (`--target tests`, `--shared` for `iOS/shared`,
`--remove`). Object ids are hash-derived, so branches that add different files
merge without id collisions.
