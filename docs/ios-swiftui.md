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

## Project files

The Xcode projects list files explicitly. Register or remove files with
`iOS/<app>/tools/add_source.py` (`--target tests`, `--shared` for `iOS/shared`,
`--remove`). Object ids are hash-derived, so branches that add different files
merge without id collisions.

## Tag Master tag detail

- **Structure.** `TagDetailScreen` (TagDetailView.swift) over `TagDetailModel`, with
  `TagSummaryModel` and `TagTracksModel` for the two pages that do work. The pages are
  `TagSummaryPage`, `TagDetailsPage`, `TagTracksPage` (with the inline `TMTrackPlayer`)
  and `TagVideosPage`; `TMListChips`, `TMListPicker`, `TMSheetMusicScreen` and
  `TMTagPlaceholder` complete it. Tags come through `TMTagLoading` (production:
  `TMCatalogTagLoader`), so tests finish loads in any order without swizzling.
- **Staging.** `TagDetailViewController` keeps the Objective-C name
  `DPTagViewController` and hosts the screen on the UIKit shell: it answers `tagId`,
  `source`, ⌘↑/⌘↓ and `canPerformAction:`, and supplies `TMDetailNavigator` (open a tag,
  a list, the sheet music reader). In a SwiftUI shell the screen can sit directly in a
  `NavigationSplitView` detail column: pass a model, set `navigator`, `expanded` and
  `screenVisible`.
- **Shared artwork.** `TMQuartetStaff(animating:)` and `TMBarberPole(compact:darkSurface:animating:)`
  (TMArtworkViews.swift) draw the vector artwork and hold still under Reduce Motion,
  in an inactive scene, or when told to. `TMBarberPole.listLoading()` and
  `.operation(_:active:)` carry the old views' accessibility labels and identifiers.
- **UIKit left in place, deliberately.** QuickLook (`TMQuickLookPreview`), the in-app
  browser (`TMSafariView`) and the tag actions sheet (`TMActionSheet`): on iOS 26
  SwiftUI's `confirmationDialog` draws an anchored bubble without Cancel, not the
  centred sheet the app had. `TMPageTabBarBridge` gives the TabView's bar the
  `page-tab-bar`/`page-<Title>` identifiers and keeps it a bottom bar on iPad.
- **Parity notes.** Bar symbols are the same `UIImage` a bar button item gets, offset
  by its alignment insets. `TMFollowsUIKitTint` reads the live UIKit tint so accent
  colours dim behind sheets as UIKit's did. Beside a list on iPad the TabView sits one
  pixel inside the column's safe area; flush, it grows into the unsafe strip under the
  floating list. That pixel is the remaining iPad difference.
