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
  returns the flat fold (inactive), which is where a two-column layout should
  split. A region's frame already holds its margins: on the Duo it is 40 points,
  the crease plus the 20 points either side iOS asks interactive content to keep
  clear, so keep content out of the frame and don't add the margins again. Folding
  changes the region, not the window's size: read the fold again when the hinge's
  state changes (`View.hingeStatus`, `iOS/shared/HingeStatusWatch.swift`). Guard
  with `#available(iOS 27.1, *)`.

## Tag Master

- **Closed, or open and upright: one stack.** The outer display, and the inner
  display held upright (669 points wide; two panes need 720, as on Android), get
  the phone's navigation stack (`TMStackRoot`).
- **Open and held wide: the book.** Home's stack fills the leading page and the
  tag the trailing one, both always showing, with a hairline divider down the
  middle of the fold's band, so the crease falls in the columns' margins
  (`TMBookRoot`, `TMShellLayout`, from the flat or folded division region). The
  tag's stack meets the display's edge, so its bar items move into the side bar,
  in three glass groups so a slip off Next tag can't change a list: the steppers;
  favorite, teachable and add to list; refresh and share. The book is
  not the iPad's `NavigationSplitView`: at 951 points UIKit's width buckets turn
  any split into an overlay that hides the list once a tag opens, whatever tile
  preference or column minimum it is given. A window where the divider can't run
  down the fold with 320 points either side (beside another app) keeps the
  stack. Reading sheet music full screen hides the list column, as on iPad; the
  music then starts at the display's edge and the crease runs through it, as
  through any scrolling content. Before a tag is chosen the trailing page has no
  charcoal band: the system drops an empty bar there.
- **Half shut like a book, both pages keep off the crease.** While the fold is
  active each column's navigation controller gains extra safe area on the crease
  side, half the fold's 40-point region (`TMCreaseInset`), so the list's
  chevrons and bar buttons and the tag's text and controls move clear of it; the
  pages' colour, the open tag's lit row and the watermark still run to the
  divider. Flat, the display is one sheet of glass and the columns keep their
  usual margins. The book reads the fold again as the hinge's state changes
  (`hingeStatus`); folding and unfolding a running app, quickly or slowly, moves
  the insets with it.
- **Half shut upright (tabletop): a music stand.** It is still the stack, but a
  tag stands up: each of its pages reads on the upper half, the one facing the
  singers, and what they play lies on the lower half under the fingers, above
  the page bar (`tmMusicStand`, `TMMusicStand.swift`). Summary, Details and
  Videos put the key note and Sheet Music there; Tracks puts the parts there and
  keeps the recording notes and the player above. A page with nothing for its
  deck, or a fold leaving either part too little room, stays whole. Standing up
  or lying down keeps the page in place, so its scroll position and a playing
  part survive. Home and the lists still scroll across the fold, as Apple's
  guidance has continuous content do.
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
  (`Fold.band(in:)`, the crease and its 20-point margins) moves both faces off it.
- **The bench instrument with its display raised.** Folded across (tabletop), the
  readout leaves the ring's hole for a display window on the upper half, and the
  ring sits alone on the lower half under the fingers. Folded like a book, the
  display takes the leading page and the ring the other. The window is sized from
  the ring (at most 1.2 × its width, 0.45 × as tall as wide), never from the half
  it sits on, so the cells stay the largest part of the face; in tabletop it sits
  just above the crease, with the keys it reports on. It is a machined part, not
  a light: One-Glow still belongs to the sounding cell. VoiceOver reads it as one
  element, "Display".
- **Targets stay full size.** The folded range rows take 44-point targets, and a
  fold that would leave the cells' touch targets under 44 points keeps the face
  whole on the larger side.
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
  their window's shape rather than the interface orientation. In tabletop the
  banner moves to the top of the upper half (`InstrumentPage`), so the ring below
  the fold gets its room and never sits against an ad.

## Testing

- CI builds with Xcode 27.1 and tests on iOS 27.0 simulators; the iPhone Duo
  simulator needs the iOS 27.1 runtime, so Duo checks are local.
- Both test hosts adopt the scene life cycle, which the iOS 27 SDK requires; test
  windows join the host's scene (`ScreenCatalog.scene`).
- Hosted tests cover the rules with synthetic windows and folds (`TMDuoTests`).
- `DuoTourUITests` (opt-in, `TEST_RUNNER_DUO_TOUR_DIR`) walks each app's main
  screens and writes captures of every screen, for checking a real pose. Set the
  pose with Device Hub's Closed / Partially Open / Open buttons in Xcode 27.1, then
  run the tour. Tag Master's `testOpeningAndClosingKeepTheTagAndPage` (with
  `TEST_RUNNER_DUO_TOUR_HANDOFF` too) plays a part on a tag's Tracks page and
  waits while the host opens and closes the device (it writes `opened` and
  `closed` into the tour directory), checking each time that the layout changed
  and the same tag, page and playing part are still there.
  `xcrun devicectl device appResize start --device <udid>
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
- In Tag Master's book the side bar's status cluster hangs over the charcoal
  band's lower edge, so its ink suits only half of it: black (on the band) after
  a tag is picked in the book, white (over the page) after a handoff. Asking
  UIKit to update the status bar style changes nothing.
- In tabletop, Pitch Perfect's Notes and Keys open with middle C at the crease.
  `List`'s `scrollTo` puts the row in the middle whatever anchor it is given, so
  opening it under the fingers needs another way in.
