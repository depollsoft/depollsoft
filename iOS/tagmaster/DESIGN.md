---
name: Tag Master iOS
description: Native singing desk, documented from the completed refresh code.
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

Charcoal ink, blue actions and the handwriting wordmark preserve Tag Master's identity. UIKit supplies readable text, grouped lists and native navigation. The barber pole is a quiet identity mark rather than a page-filling backdrop.

This record covers `iOS/tagmaster` only. See the [Android counterpart](../../Android/TagMaster/DESIGN.md), [product context](../../PRODUCT.md) and [confirmed surface brief](../../.impeccable/surfaces/id-tagmaster-src-main-res-layout-tagmasterview-xml.md). The brief owns singing-first Home/Browse/Search strategy and seed `a858fb14`, candidate 6. Root design documents belong to Pitch Perfect.

**Key Characteristics:**

- Handwriting for identity, native semantic type for reading and controls.
- Blue actions on appearance-aware grouped surfaces.
- Readable panes and explicit large-text alternatives.

Evidence comes from `tagmaster/TMTheme.swift`, `TMRootController.swift`, `DPTagViewController.m`, `DPTagPageControllerBase.m` and the Home, Search, Summary and Details controllers. Current implementation outranks pending-review comments or earlier plans.

The [final iOS verdict](../../.impeccable/review/tagmaster-iOS-verdict.md) records ship only for the listed Summary-width/rating and attribution fixes. It does not certify the entire app. Settings, populated repertoire, chart/player and recovery states lack new capture verification there; hardware audio, haptics, VoiceOver gestures, signed-in sync and remote services remain unverified. This documentation pass ran no builds, tests or reviews.

## Colors

### Primary

Blue `TMTheme.tint` marks actions and links. The frontmatter preserves the actual UIColor RGB inputs rather than rounding them to comment hex values. Each custom pair switches on `userInterfaceStyle == .dark`.

### Secondary

`TMTheme.affirmative` is a green positive-availability marker, not another navigation accent.

### Neutral

`TMTheme.ink` supplies heading and wordmark contrast. Other neutrals remain native semantic APIs, not fixed swatches:

- `primaryText` is `UIColor.label`; `secondaryText` is `UIColor.secondaryLabel`.
- `canvas` is `UIColor.systemGroupedBackground`; `surface` is `UIColor.secondarySystemGroupedBackground`.
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

Spacing tokens are UIKit points, not CSS pixels. Shared controls use a 44pt minimum target. Home uses inset-grouped automatic-height rows with readable-width margins; its 72pt row height is an estimate, not a fixed height.

`DPTagPageControllerBase` centers padded scroll content, caps its outer width at 700pt and prefers full available width at priority 999. Horizontal padding is `spaceL`, vertical padding `spaceS`, with 4pt scroll-edge constraints. Do not replace this with intrinsic-width centering.

`TMRootController` retains native tabs. On iOS 18+, `.tabSidebar` uses tiled layout; the sidebar is shown only at regular horizontal size class and root width at least 1000pt. Narrower windows and older OS versions retain native tab behavior.

Tag detail splits only at regular width, at least 760pt, and outside accessibility text categories. Summary occupies 42% of the view; Details, Tracks or Videos occupy the other pane. Otherwise Summary is one of four selectable sections. The divider is one physical pixel.

**The Readable Pane Rule.** Use available window width and text category, not device identity, to choose detail layout.

## Elevation & Depth

Grouped canvas and content tones separate regions. Navigation and tab bars use UIKit default-background appearances. The shared theme defines no custom shadow or elevation scale; native sheets and menus retain their platform treatment.

## Shapes

Inset-grouped tables, native segmented controls and configured buttons own their shapes. No app-wide numeric corner-radius scale is defined. Use native SF Symbols for action icons rather than inventing a replacement icon family.

## Components

- Home exposes native action rows, Favorites editing and a Settings utility in the navigation bar. Each root destination has its own `UINavigationController`; programmatic entry can focus Home or Search.
- Search uses a minimal `UISearchBar`, a filled Search Tags button and grouped filters. Button insets are `spaceM` vertically and `spaceL` horizontally.
- `TMAdaptiveChoiceView` presents segments normally and a bordered menu button at accessibility categories. Choices retain their labels and selected state; menu insets match the Search button.
- Summary keeps all five rating stars and numeric text together. At accessibility sizes, Rate moves below them and relaxes horizontal hugging so the pane does not collapse.
- Tag detail offers Share tag, Add to a list and Refresh tag. Sharing anchors its native popover to the share bar button.
- `TMEmptyStateView` pairs a symbol, title and message with an optional bordered recovery action. Its stack caps at 420pt and uses `spaceXL` outer clearance.
- `TMTheme.saved()` requests native success feedback; `selected()` requests selection feedback. No custom animation duration is defined by these helpers. Native navigation and presentation own their transitions.

The existing `screenbackground.png` is reused as an aspect-fit template image, tinted with `UIColor.label` at 0.10 alpha and excluded from interaction and accessibility. The shipped handwriting font and rasters remain unchanged. This refresh documents reuse, not a new image license or a replacement asset set.

## Do's and Don'ts

- Do preserve Home, Browse, Search, Favorites, Teachable Tags, Summary, Details, Tracks and Videos terminology alongside BarbershopTags.com attribution.
- Do preserve semantic text colors and large-text menu, field-row and rating alternatives.
- Don't import Pitch Perfect's visual rules into Tag Master.
- Don't force a sidebar or split detail into a narrow or accessibility-size window.
- Don't canonize isolated legacy styling or unsampled shared-library internals as reusable system rules.
