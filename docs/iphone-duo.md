# iPhone Duo

iPhone Duo (iOS 27.1, October 2026) is Apple's foldable: a 5.4-inch outer display
(466 × 678 pt, compact width) when closed and a 7.6-inch inner display
(951 × 669 pt held like a book, 669 × 951 pt upright; regular both ways) when open.
Half open, a fold divides the inner display. Both apps support it through the same
means they use for every other window: size classes, the window's own geometry and
the iOS 27.1 reserved-region API. Neither checks for the device.

## Rules for both apps

- **Size classes decide structure.** A window regular in both directions (an open
  Duo, an iPad) is tablet-class; anything else is phone-class, a large phone in
  landscape included. The outer display behaves like an iPhone, and the inner
  display ignores the app's supported orientations.
- **No `UIScreen.main`.** A Duo has two screens, so code reads the view's
  `displayScale`, its own size, or its scene's screen.
- **Vertical bars.** Built with the iOS 27.1 SDK, navigation, tool and tab bar
  items move into a bar along the side on the outer display and in landscape on
  the inner one. Items there stand on the system's glass over the page, so they
  draw in label colours, never in the white a charcoal bar needs. Read
  `toolbarVerticalEdge` (SwiftUI) or `verticalBarEdge` (UIKit) to tell.
- **Safe areas are asymmetric.** The side bar, the status corner and the camera
  sit on one edge; inset each side on its own.
- **Folds come from reserved regions.** `GeometryProxy.reservedRegions(kind: .division)`
  returns the fold while the device is half open; with `.includeInactive` it also
  returns the flat fold (zero width), which is where a two-column layout should
  split. Guard with `#available(iOS 27.1, *)`.

## Tag Master

- **Closed, or open and upright: one stack.** The outer display, and the inner
  display held upright (669 points wide; two panes need 720, as on Android), get
  the phone's navigation stack (`TMStackRoot`).
- **Open and held wide: the book.** Home's stack fills the leading page and the
  tag the trailing one, both always showing, with a hairline divider down the
  middle of the fold's band, so the crease falls in the columns' margins
  (`TMBookRoot`, `TMShellLayout`, from the flat or folded division region). The
  tag's stack meets the display's edge, so its bar items (steppers, favorite,
  teachable, add to list, refresh, share) move into the side bar. The book is
  not the iPad's `NavigationSplitView`: at 951 points UIKit's width buckets turn
  any split into an overlay that hides the list once a tag opens, whatever tile
  preference or column minimum it is given. Reading sheet music full screen
  hides the list column, as on iPad. Before a tag is chosen the trailing page
  has no charcoal band: the system drops an empty bar there.
- **Half shut like a book, both pages keep off the crease.** While the fold is
  active each column's navigation controller gains extra safe area on the crease
  side, half the band plus the 20-point margin iOS asks for (`TMCreaseInset`),
  so the list's chevrons and bar buttons and the tag's text and controls move
  clear of it; the pages' colour and the watermark still run to the divider.
  Flat, the display is one sheet of glass and the columns keep their usual
  margins. The fold's region turns active a moment after the hinge reports the
  change, so the book re-reads it on every hinge update (`TMHingeWatch`).
- **Half shut upright (tabletop): the stack, unchanged.** The bars sit at the
  top and bottom, far from the crease; the pages between them scroll, and
  scrolling content does not move around a fold (Apple's guidance), so a control
  that scrolls onto the crease (Sheet Music, say) is scrolled off it.
- **Opening, closing and turning carry the tag.** Opening moves the tag on top of
  the stack, with its sheet music, beside the list (`TMRouter.setExpanded(true)`);
  closing puts the tag beside the list on top of the stack (`TMRouter.useStack()`).
  It is the same `TagDetailModel` both ways, so its page survives, and a part
  playing on the Tracks page keeps playing: the page stops the player only once
  no page showing the tag is left (`TagTracksModel.carriedAcross`).
- **Menus float on the open display.** The Add to list picker and the tag's
  action sheet anchor to their button as popovers in a window regular both ways
  (`UITraitCollection.tmFloatsPopovers`); a closed Duo gets the phone's sheets.
- **The charcoal bar.** The title band stays charcoal. Items that move into the
  side bar draw in label ink (`TMBarButton.ink(vertical:)`), and the refresh
  barber pole there is drawn for a light surface.

## Pitch Perfect

DESIGN.md's *Folded (iPhone Duo)* section is the design record; in short:

- **Only a fold changes the face.** Closed, open flat, and on every other device
  the pitch pipe draws exactly as before. Half open, the active division region
  (`Fold.band(in:)`) moves both faces off the crease and its 20-point margins.
- **The bench instrument with its display raised.** Folded across (tabletop), the
  readout leaves the ring's hole for a display window on the upper half, and the
  ring sits alone on the lower half under the fingers. Folded like a book, the
  display takes the leading page and the ring the other. The window is sized from
  the ring (at most 1.2 × its width, 0.45 × as tall as wide), never from the half
  it sits on, so the cells stay the largest part of the face. It is a machined
  part, not a light: One-Glow still belongs to the sounding cell.
- **Classic grid** opens a gap at the fold (rows 2|3 or columns 2|3) and splits
  its well the same way: readout on one side, the C to B / F to E choice on the other.
- A fold too near the face's edge (beside another app) moves the whole face to
  the larger side instead of splitting it. Folding crossfades in 0.3 s (a cut
  with Reduce Motion).
- **Bars.** The tab bar and bar buttons stand down the side where the Duo puts
  them. Settings, Log In and Add songs keep one horizontal bar (a lone control,
  or words); Keys' Major/Minor stays horizontal.
- **Sheets and banners.** An open Duo is regular both ways, so sheets centre as
  on iPad (`isRegularSpace`); banners still show, as on any phone, sized from
  their window's shape rather than the interface orientation.

## Testing

- CI builds with Xcode 27.1 and tests on iOS 27.0 simulators; the iPhone Duo
  simulator needs the iOS 27.1 runtime, so Duo checks are local.
- Both test hosts adopt the scene life cycle, which the iOS 27 SDK requires; test
  windows join the host's scene (`ScreenCatalog.scene`).
- Hosted tests cover the rules with synthetic windows and folds (`TMDuoTests`).
- `DuoTourUITests` (opt-in, `TEST_RUNNER_DUO_TOUR_DIR`) walks each app's main
  screens and writes captures of every screen, for checking a real pose. Set the
  pose with Device Hub's Closed / Partially Open / Open buttons in Xcode 27.1, then
  run the tour. `xcrun devicectl device appResize start --device <udid>
  --preferred-size 951x669` puts a launched app in a window the inner display's
  size (capture it with `xcrun simctl io <udid> screenshot --display=5`), without
  the side bars or a fold.
- `xcrun simctl io <udid> screenshot --display=1` captures the outer display and
  `--display=3` the inner one.
- App Store Connect lists iPhone Duo screenshot sizes (outer 1398 × 2034, inner
  2007 × 2853) and will require them from April 2027 for apps built with the iOS
  27.1 SDK; `scripts/release/capture.py` does not capture them yet.

## Known issues

- Run on the closed Duo simulator, a few hosted tests fail because they assume
  an iPhone's geometry (no side bar inset, a bottom tab bar): nine in
  Pitch Perfect, and Tag Master's picker-placement check, which is 4 points off a
  UIKit table on the outer display. CI runs on iPhone simulators, where they pass.
- In the iOS 27.1 simulator, Pitch Perfect's Settings gear can lose its symbol
  in the side bar (the glass button stays) after opening upright, folding to
  tabletop and closing. It happens with the fold code switched off and with any
  way of supplying the icon, so it looks like the system's; simple round trips
  don't do it, and Tag Master's bar items survive the same sequence.
