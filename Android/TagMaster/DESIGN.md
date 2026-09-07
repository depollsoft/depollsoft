---
name: Tag Master Android
description: Native singing desk, documented from the completed refresh code.
colors:
  darker: "#373737"
  tm-primary: "#00637D"
  tm-primary-night: "#5FD4F4"
  tm-on-primary: "#FFFFFF"
  tm-on-primary-night: "#003544"
  tm-primary-container: "#B6EAFF"
  tm-primary-container-night: "#004D61"
  tm-on-primary-container: "#001F28"
  tm-on-primary-container-night: "#B6EAFF"
  tm-secondary: "#474747"
  tm-secondary-night: "#C4C7C8"
  tm-secondary-container: "#DCE4E8"
  tm-secondary-container-night: "#474747"
  tm-surface: "#FBFCFE"
  tm-surface-night: "#191C1D"
  tm-on-surface: "#191C1D"
  tm-on-surface-night: "#E1E3E4"
  tm-surface-variant: "#DBE4E7"
  tm-surface-variant-night: "#3F484B"
  tm-on-surface-variant: "#3F484B"
  tm-on-surface-variant-night: "#BFC8CB"
  tm-surface-container: "#EFF1F3"
  tm-surface-container-night: "#252829"
  tm-outline: "#6F797C"
  tm-outline-night: "#899295"
  tm-outline-variant: "#BFC8CB"
  tm-outline-variant-night: "#3F484B"
  tm-error: "#BA1A1A"
  tm-error-night: "#FFB4AB"
typography:
  wordmark:
    fontFamily: "Wickhop Handwriting"
spacing:
  tm-space-xs: "4dp"
  tm-space-s: "8dp"
  tm-space-m: "12dp"
  tm-space-l: "16dp"
  tm-space-xl: "24dp"
  tm-space-xxl: "32dp"
---
# Design System: Tag Master Android

## Overview

**Creative North Star: "The singing desk"**

A charcoal action bar and handwriting title preserve Tag Master's identity. Blue marks actions; Material 3 supplies native fields, buttons and navigation. The original full-page barber-pole vector sits behind the workspace, at its original light/night tints. The user rejected icon-scale substitution and global tabs.

This record covers `Android/TagMaster` only. See the [iOS counterpart](../../iOS/tagmaster/DESIGN.md), [product context](../../PRODUCT.md) and [confirmed surface brief](../../.impeccable/surfaces/id-tagmaster-src-main-res-layout-tagmasterview-xml.md). The brief owns singing-first Home/Browse/Search strategy and seed `a858fb14`, candidate 6. Root design documents belong to Pitch Perfect.

**Key Characteristics:**

- Charcoal identity chrome with blue Material actions.
- Readable-width content beside labeled native navigation.
- Handwriting titles paired with native reading and control typography.

Evidence comes from `src/main/res/values/{colors,themes,dimens}.xml`, night and width qualifiers, layouts, and `src/main/java/depollsoft/tagmaster/{ReadableWidthLayout,DeskUi,Utilities,MeHeaderView}.kt`.

Historical refresh evidence, superseded for visual/navigation direction by [the Android character correction](../../.impeccable/review/character-correction/android-report.md). The [final Android verdict](../../.impeccable/review/tagmaster-android-verdict.md) records ship only for the PDF page-continuity fix, not the whole app. Its synthetic pages prove order, not engraving quality. Active-player, video and tablet-detail final captures remain absent; tablet evidence used an 800dp API36 emulator simulation, not tablet hardware. Authentication, sync, physical audio, TalkBack, haptics, motion and complete posture/state coverage remain unverified. The current separate-list and compact-row changes are covered by the [Android density report](../../.impeccable/review/density/android-report.md). That report distinguishes passing test evidence from the unresolved preference-content restoration check.

## Colors

### Primary

The frontmatter records selected reusable resource pairs exactly. Unsuffixed tokens come from `values/colors.xml`; `-night` tokens come from `values-night/colors.xml`. Blue primary/on-primary roles carry the filled Find a tag action. Container pairs supply Material tonal treatments.

### Secondary

Neutral secondary roles support tonal actions without introducing another brand hue. `themes.xml` also wires their on-colors and the error container/on-error pairs directly from the resource files.

### Neutral

Surface and on-surface roles carry canvas and reading text; background/on-background resources have the same values. Surface-container and variant roles separate groups. Outline roles support native boundaries. The action bar keeps `darker` in both appearances, with white title text, control color `#F2F2F2` and secondary text `#D8D8D8`.

**The Native Role Rule.** Request theme attributes such as `colorPrimary`, `colorSurface` and `colorOnSurface`; let the single `Theme.Material3.DayNight` mapping select resource values.

Night mode uses explicit charcoal and blue resources. These custom palettes do not implement a separate increased-contrast scheme; DayNight is not a claim of automatic contrast adaptation or dynamic wallpaper color.

## Typography

`Utilities.makeTitleString` applies the existing `assets/fonts/wickhop-handwriting.ttf` through `CustomTypefaceSpan`. Body text and controls inherit native Material text appearances; do not substitute guessed fixed sizes or a web font stack.

- Action-bar title inherits `TextAppearance.Material3.TitleLarge`, with white text.
- Section headings inherit `TitleMedium`, with `colorOnSurface`.
- Field labels inherit `LabelMedium`, with `colorOnSurfaceVariant`.
- Field values inherit `BodyMedium`, with `colorOnSurface`.
- Home actions use `TitleMedium` with start-aligned text and icons.

**The Native Reading Rule.** Keep handwriting in identity and use Material text appearances for reading and controls.

Native sp-based typography follows font scale. Scroll containers, wrap-content rows and 48dp minimum targets allow growth. Search label/spinner rows remain horizontal with 2:3 weights; no accessibility-font breakpoint or automatic vertical restacking is implemented there. Do not claim every large-text state is proven.

## Layout

Spacing tokens are density-independent Android dp. Default `tm_gutter` is 16dp and `tm_content_max_width` is 600dp. At the available-width qualifier `w600dp`, these become 24dp and 640dp. This is not a smallest-device-width qualifier.

`ReadableWidthLayout` measures content against the cap, retains the parent's exact outer width and centers visible children within the capped measure. Home, Search and reading layouts use this wrapper rather than stretching prose across a tablet.

Home hosts only HomeFragment. Find and Browse push their existing activities. Catalog collections use a labeled native Spinner. Tag Summary opens Learning tracks, Details, or Videos as a local content push. At 720dp available width with font scale below 1.5, Summary stays beside the selected material. Accessibility text uses one column. Native Back returns from material to Summary, then to the caller.

**The Available Width Rule.** Cap Home's reading column; use width for Summary plus material, never a global rail. PoleBackground draws one aspect-fit original asset behind both panes.

`DeskUi.applyDeskInsets` pads left/right for system bars and cutouts, and bottom for the larger of system-bar or IME insets when enabled. The action bar handles its own top inset. Child Up follows the existing stack; a task-root child returns to `MeActivity`.

## Elevation & Depth

The charcoal action bar explicitly has 0dp elevation. Material tonal surfaces and native widget states supply depth elsewhere; dialogs use the Material alert-dialog overlay. There is no custom app shadow scale to transfer between components.

## Shapes

Find a tag uses primary fill. Browse, Random Tag and Open tag ID use quiet native text actions. Search uses the Material 3 outlined text-field shape. Navigation and icon buttons retain their native style shapes. No app-wide custom numeric radius scale is defined.

## Components

- Home actions use full-width `MaterialButton` controls with 48dp minimum height, `tm-space-s` top margin, `tm-space-l` horizontal padding and `tm-space-m` icon padding. Random Tag reports inline progress, disables itself while loading and offers Snackbar retry after failure.
- Open Tag ID uses an outlined numeric `TextInputEditText` in a native alert. Invalid or nonpositive input keeps the dialog open and sets the field error.
- Search combines outlined text input, a full-width Search button and native Spinners for sorting, sheet music, learning tracks, parts and collection.
- No global or detail navigation bar/rail. Home pushes Browse/Search, then Tag. Summary contains labeled material actions; Settings remains a Home toolbar utility.
- The part player has checkable Play/Pause and Stop icon buttons, a loading indicator, and labeled position/balance SeekBars. Play/Pause selects `ic_pause` when checked and `ic_play` otherwise; targets use `tm_touch_target`.
- `confirmHaptic` requests `CONFIRM` on API 30+ and `VIRTUAL_KEY` earlier. Pager and widget transitions use native behavior; these helpers define no custom timing curve or duration.

The workspace uses the original `ic_barberpole.xml`, 299dp by 513dp, with original `#11333333` light and `#11DDDDDD` night tint. `PoleBackground` aspect-fits it behind Home, catalog/search/results, tag content, Settings and Teachable Tags. The rejected refresh's 24dp `ic_barberpole_mark.xml` is not the workspace background. Player icon selectors and the handwriting font remain unchanged.

## Do's and Don'ts

- Do preserve Home, Browse, Search, Favorites, Teachable Tags, Summary, Details, Tracks and Videos terminology alongside BarbershopTags.com attribution.
- Do use DayNight theme roles, readable-width wrappers and native labeled controls.
- Don't import Pitch Perfect's visual rules into Tag Master.
- Don't restore global tabs/rails, collection tabs, detail tabs, or a tiny-footer pole.
- Don't canonize isolated legacy styling or unsampled shared-library internals as reusable system rules.

## Saved lists and scan density

Home keeps discovery actions, Settings and attribution. Your lists contains two equal, full-width disclosures with inline counts: Favorites and Teachable Tags. There are no inline saved-tag feeds or Home editing controls. Both entries work when empty. FavoritesActivity reuses FavoriteTagItemView and its existing long-press move up, move down and removal operations. TeachableTagsActivity retains its own operations and now titles the page Teachable Tags. Each activity keeps its scroll when returning from Tag. The pole remains behind loaded, loading and empty lists.

Catalog and saved rows use a TitleMedium title, an optional nonblank alternate that differs from the title, and one BodyMedium ID/material-status line. Availability is read-only text, not checkbox-shaped controls. The baseline is 8dp top/bottom padding and a 4dp line gap, targeting 60–72dp for two lines at default type. Heights wrap, titles are not ellipsized, and interactive rows retain a 48dp minimum. Read-only markers have no independent touch target. Rating remains in Summary; posted date and downloads remain in Details. Filters, sorts, global spacing tokens, footer hit areas, player and PDF flows are unchanged.

Multiple labels per tag and many named lists are future intent only. The repeated disclosure style can accommodate future entries, but this change implements only the two existing built-ins. No custom labels, list-management placeholders, model, migration or sync changes.

Density verification and measured results: [Android density report](../../.impeccable/review/density/android-report.md).
