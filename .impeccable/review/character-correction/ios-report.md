# iOS character correction

Status: corrected native screens ready for Main and user review, not a blanket shipping verdict. No commit, push or remote review changes.

## Show these six captures first

All are actual Simulator captures, resized with `sips -Z 1000`, under 300 KB. Read one image per tool call.

- `ios-phone-home.png`: light Home, original pole, wordmark, Find/Browse, compact Random/ID, Favorites then Teachable.
- `ios-phone-tag.png`: light Summary with pitch, Sheet Music and pushed-material rows.
- `ios-phone-tracks-dark-large.png`: actual dark appearance, accessibility XXXL, pushed Tracks and native Back. Recording notes scroll with parts.
- `ios-tablet-home.png`: landscape Home without sidebar.
- `ios-tablet-tag.png`: landscape Summary and Tracks sharing one pole.
- `ios-tablet-browse.png`: final populated Classic catalog with readable titles/metadata, ordinary rows and Collection menu.

Paths are relative to `.impeccable/review/character-correction/`. The pole was visually verified in these captures, not merely checked in the asset catalog. Main must show them to the user before any review update.

## Implemented

- Verified exact worktree root before editing. Code and documentation edits are confined to `iOS/tagmaster`.
- Restored original `screenbackground.png`, untouched in light appearance. Dark uses a white template with the raster's existing alpha, no second fade. The full-page aspect-fit background is shared across tablet panes. Removed footer pole.
- Replaced TMRootController's three tab stacks/sidebar with one UINavigationController, retaining Home/deep-link helper APIs. Find pushes existing Search; Browse is explicit on Home. No giant unified catalog rewrite.
- Collection and filter choices use labeled native menus, preserving existing cached query controllers.
- Summary remains the tag workspace. Learning Tracks, Details and Videos push existing controllers on phone. Wide regular windows retain Summary beside material; accessibility sizes use one column. Pushed pages attach the shared busy indicator and native title/Back.
- Kept native body sizes, wordmark, blue/charcoal, pitch/chart code, rating XXXL geometry, attribution labels, list storage, model/network code, media player, retry behavior and existing asset bytes.
- Corrected actual review failures: recording-note scrolling; full secondary-text contrast over the pole; a hidden legacy busy-grid producing 200,000-point catalog rows; spinner intrinsic sizing compressing titles; and UINavigationController's deferred parent removal during material expansion. Those last defects have hard regression assertions.
- Updated only the Tag Master DESIGN.md and sidecar to remove rejected footer/tab/sidebar rules.

## Checks and passing counts

Across focused runs: all 29 TMRefreshTests methods passed, including two added geometry/adaptive tests. The 27-method first run had three failing navigation methods, four assertions, corrected by waiting for native transitions in the tests. Only failed or subsequently changed checks were rerun. This is incremental evidence, not one final rerun of the entire suite.

Six native UI test/device cases passed: three TMDeskUITests methods on both supplied simulators. Coverage includes Home to Search to a named result to Tag to Tracks to native Back, preserved `Smile` query, known missing tracks without OR fallback, Collection: Classic, ID 1, summary width/pitch target, attribution roles, anchored share sheet, accessibility XXXL rating geometry, pushed-material Back, three visible catalog rows with bounded heights, actual title height and selected Videos after rotation. Tablet accessibility was light; actual dark coverage was rerun explicitly on phone because launch arguments did not override Simulator appearance.

Retained unit checks cover list keys/state, teachable notifications, deep-link helper routing, collection mapping, player stop on dismissal/interruption, failed later-page retry, summary widths and XXXL rating, attribution semantics and contrast, and single font scaling. New unit geometry checks cover 320/402/1000-point rows and Videos retained across 1000 -> 600 -> 1000-point navigation layouts.

Debug compilation succeeded in the native test builds. `git diff --check -- iOS/tagmaster` passed. No changes to original raster, Barbershop models/network or TMTrackPlayerController. Existing Xcode source registration was sufficient; no new source file was added.

## Commands and full logs

All xcodebuild shell calls used a 1200-second timeout, complete log redirection and `set -o pipefail`, except the first tablet test-without-building had no pipeline. Standard command:

```sh
xcodebuild -workspace iOS/iOS.xcworkspace -scheme tagmaster \
  -configuration Debug -derivedDataPath /tmp/tm-dd \
  -destination 'platform=iOS Simulator,id=<UDID>' \
  -parallel-testing-enabled NO -only-testing:<target/class/method> \
  test -resultBundlePath /tmp/ios-character-<run>.xcresult
```

Subsequent device-only runs used `test-without-building`. Explicit dark configuration:

```sh
xcrun simctl ui C8B74E44-94F7-4CED-A47F-DFF98E34237B appearance dark
xcrun xcresulttool export attachments --path <result>.xcresult --output-path <export>
sips -Z 1000 <exported-image> --out <review-image>
```

Phone: `C8B74E44-94F7-4CED-A47F-DFF98E34237B`, iPhone 17 Pro. Tablet: `55A9555A-9714-4FE0-8F0D-1035652526F9`.

Full logs here: `ios-build.log`, `ios-phone-tests.log`, `ios-phone-confirm.log`, `ios-tablet-initial.log`, `ios-tablet-confirm.log`, `ios-geometry.log`, `ios-geometry-fix.log`, `ios-adaptive-fix.log`, `ios-phone-catalog.log`, `ios-tablet-catalog.log`, `ios-cell-final.log`, `ios-catalog-readable.log`, `ios-dark-final.log`. Result bundles remain under `/tmp/ios-character-*.xcresult`; the initial phone bundle was relocated there to avoid leaving thousands of generated files in the review tree.

The initial native capture batch was followed by a correction batch. Further runs were failure-driven, including real catalog sizing, adaptive containment, and a falsely labeled dark capture. These failures extended the requested time box; no theme exploration or aesthetic iteration followed.

## Limits requiring follow-up

- No hardware audio/haptics, VoiceOver reading order/edge-swipe, signed-in sync/migration, actual cold/warm OS deep-link traversal, filtered Random, Favorites/Teachable UI persistence, Settings interaction, PDF page traversal, track download/play/pause/retry or video playback was newly exercised end to end. Relevant code was preserved; deep-link routing and player stop/retry have unit evidence, not full media certification.
- Rotation and synthetic navigation-width changes passed, but live Stage Manager/Split View resizing and scroll-offset retention were not independently asserted. Browse retains controller instances and Search retains its form through Back.
- Custom text contrast was checked through existing tests and opaque secondary colors. No new complete VoiceOver, increased-contrast or reduced-transparency audit.
- Remote catalog results are live, not fixtures. Hard route assertions use named known content; no tests accept arbitrary UI alternatives.
- Shared-tree hooks reported Android worker edits/autofixes while iOS commands ran. No Android edits were intentionally made by this worker; Main should inspect the platform worker's resulting diff.

Main should review these six images with the user, then decide whether more acceptance coverage is needed. Do not treat passing tests as approval of character.
