---
name: Tag Master iOS
description: Native Tag Master character correction.
colors:
  ink-light: "color(srgb 0.216 0.216 0.216)"
  ink-dark: "color(srgb 0.96 0.96 0.96)"
  tint-light: "color(srgb 0 0.404 0.576)"
  tint-dark: "color(srgb 0.361 0.780 0.941)"
  affirmative-light: "color(srgb 0.09 0.47 0.27)"
  affirmative-dark: "color(srgb 0.36 0.82 0.55)"
typography:
  wordmark:
    fontFamily: "wickhop handwriting"
    fontSize: "24pt"
spacing:
  spaceXS: "4pt"
  spaceS: "8pt"
  spaceM: "12pt"
  spaceL: "16pt"
  spaceXL: "24pt"
---
# Design System: Tag Master iOS

## Overview

**Creative North Star: "The singing desk"**

Charcoal ink, blue actions and the handwriting wordmark preserve Tag Master's identity. UIKit supplies readable text, ordinary rows and native push navigation. The original barber pole fills the page behind content.

This record covers `iOS/tagmaster` only. See the [Android counterpart](../../Android/TagMaster/DESIGN.md), [product context](../../PRODUCT.md) and [confirmed surface brief](../../.impeccable/surfaces/id-tagmaster-src-main-res-layout-tagmasterview-xml.md). The user’s character-correction contract overrides the rejected tabs and footer-mark direction. Root design documents belong to Pitch Perfect.

**Key Characteristics:**

- Handwriting for identity, native semantic type for reading and controls.
- Blue actions over the original page-scale pole.
- Readable panes and explicit large-text alternatives.

Evidence comes from `tagmaster/TMTheme.swift`, `TMRootController.swift`, `DPTagViewController.m`, `DPTagPageControllerBase.m` and the Home, Search, Summary and Details controllers. Current implementation outranks pending-review comments or earlier plans.

The [character-correction report](../../.impeccable/review/character-correction/ios-report.md) records the actual corrected screenshots, focused native test runs and remaining limits. The earlier verdict did not approve the rejected character and navigation direction. User review of the corrected screens is still required.

## Colors

### Primary

Blue `TMTheme.tint` marks actions and links. The frontmatter preserves the actual UIColor RGB inputs rather than rounding them to comment hex values. Each custom pair switches on `userInterfaceStyle == .dark`.

### Secondary

`TMTheme.affirmative` is a green positive-availability marker, not another navigation accent.

### Neutral

`TMTheme.ink` supplies heading and wordmark contrast. Other neutrals remain native semantic APIs, not fixed swatches:

- `primaryText` is `UIColor.label`; secondary text is opaque charcoal in light mode and pale gray in dark mode for readability over the pole.
- `canvas` is `UIColor.systemBackground`; `surface` is `UIColor.secondarySystemGroupedBackground`.
- `separator` is `UIColor.separator`; the Search button foreground uses `UIColor.systemBackground`.

**The Semantic Surface Rule.** Resolve native semantic colors at use time; do not replace them with sampled light-mode hex values.

UIKit owns semantic-color adaptation. Custom ink, tint and affirmative providers have no increased-contrast branch; do not claim automatic increased-contrast support for them.

## Typography

The wordmark token is its base size, scaled with `UIFontMetrics` for `.headline`. `wordmarkFont` falls back to the system semibold face if the shipped handwriting face cannot load.

`TMTheme.font` uses the preferred descriptor with a weight trait, without scaling twice. These roles deliberately have no fixed point-size substitutes:

- Screen title uses `.title1`, bold; group title uses `.title3`, semibold.
- Field label uses `.subheadline`, semibold; body uses preferred `.body`.
- Metadata uses preferred `.footnote`.
- Navigation title uses `.headline`, semibold; large navigation title uses `.largeTitle`, bold.

**The Native Reading Rule.** Keep handwriting in identity and use preferred UIKit text styles for reading and controls.

Labels opt into Dynamic Type. `TMFieldRow` has a 132pt label column at ordinary sizes and switches to a leading-aligned vertical stack at accessibility categories. Attribution without a URL is unlimited-line static `UILabel` text in `primaryText`; only a website becomes a link.

## Layout

Spacing tokens are UIKit points, not CSS pixels. Shared controls use a 44pt minimum target. Home uses plain automatic-height rows with readable-width margins; its 72pt row height is an estimate, not a fixed height.

`DPTagPageControllerBase` centers padded scroll content, caps its outer width at 700pt and prefers full available width at priority 999. Horizontal padding is `spaceL`, vertical padding `spaceS`, with 4pt scroll-edge constraints. Do not replace this with intrinsic-width centering.

`TMRootController` is one UINavigationController. Home pushes Browse or Search; results and IDs push a tag. Native Back returns to the caller. No global tabs or sidebar.

Tag detail splits only at regular width, at least 760pt, and outside accessibility text categories. Summary occupies 46% of the view; Details, Tracks or Videos occupy the other pane. Otherwise Summary pushes material pages from labeled rows. Existing page instances retain material state through width changes. The divider is one physical pixel.

**The Readable Pane Rule.** Use available window width and text category, not device identity, to choose detail layout.

## Elevation & Depth

One aspect-fit original pole sits behind each workspace, including both tablet panes. Navigation bars use UIKit default-background appearances. The shared theme defines no custom shadow or elevation scale; native sheets and menus retain their platform treatment.

## Shapes

Plain tables, native menus and configured buttons own their shapes. No app-wide numeric corner-radius scale is defined. Use native SF Symbols for action icons rather than inventing a replacement icon family.

## Components

- Home exposes native action rows, Favorites editing and a Settings utility in the navigation bar. Find and Browse are visible Home rows, followed by compact Random and ID actions, Favorites, then Teachable Tags.
- Search uses a minimal `UISearchBar`, a filled Search Tags button and grouped filters. Button insets are `spaceM` vertically and `spaceL` horizontally.
- `TMAdaptiveChoiceView` presents a labeled native menu button at all text sizes. Choices retain their labels and selected state; menu insets match the Search button.
- Summary keeps all five rating stars and numeric text together. At accessibility sizes, Rate moves below them and relaxes horizontal hugging so the pane does not collapse.
- Tag detail offers Share tag, Add to a list and Refresh tag. Sharing anchors its native popover to the share bar button.
- `TMEmptyStateView` pairs a symbol, title and message with an optional bordered recovery action. Its stack caps at 420pt and uses `spaceXL` outer clearance.
- `TMTheme.saved()` requests native success feedback; `selected()` requests selection feedback. No custom animation duration is defined by these helpers. Native navigation and presentation own their transitions.

The original `screenbackground.png` is used untouched in light mode, aspect-fit at page scale over systemBackground. Dark mode uses a white template at full view alpha, retaining the raster’s original alpha. No second fade, footer mark or opaque pane covers it. The shipped handwriting font and rasters remain unchanged. This refresh documents reuse, not a new image license or a replacement asset set.

## Do's and Don'ts

- Do preserve Home, Browse, Search, Favorites, Teachable Tags, Summary, Details, Tracks and Videos terminology alongside BarbershopTags.com attribution.
- Do preserve semantic text colors and large-text menu, field-row and rating alternatives.
- Don't import Pitch Perfect's visual rules into Tag Master.
- Don't restore global tabs, a sidebar, local tag tabs or a tiny footer pole. Accessibility sizes use one column.
- Don't canonize isolated legacy styling or unsampled shared-library internals as reusable system rules.
