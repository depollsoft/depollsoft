# Tag Master Android round 3 (brand color, detail buttons, title clipping)

Implemented by Astra from the coordinator's direction; the agent's session ended before it wrote this report, so the coordinator verified the final tree and wrote it from the progress log (`progress-android-round3.md`) and fresh captures.

## What changed

- `res/values/colors.xml`, `res/values-night/colors.xml`: charcoal chrome tokens (`brand_chrome` #373737, `brand_on_chrome`, `brand_chrome_accent` #5AC8FA), `status_available` green, blue primary roles (#007AA3 light / #5AC8FA dark, containers #CDE9F7 / #004D6B); the pink secondary roles now alias the blue family. New `res/color/` selectors for tab text/icons.
- `res/values/themes.xml`: charcoal status bar with light icons in both appearances; `ThemeOverlay.TagMaster.Chrome` (white title, icons, Up, overflow on the bar) and `ThemeOverlay.TagMaster.Popup` (menus follow the app appearance, not the bar).
- `res/layout/app_toolbar.xml`: charcoal `AppBarLayout`/`MaterialToolbar`, no lift color change, white title/navigation tint, popup theme.
- `EdgeToEdge.kt`: paints the status-bar inset charcoal (the shared `RichApplication` consumes the inset before the app bar sees it) and forces light status-bar icons.
- Tabs on Browse and Detail: charcoal background, white/70% text, `#5AC8FA` indicator, matching icon selector.
- `res/layout/tagsummaryview.xml` + `layout-land`: Sheet Music is a filled primary button with icon; Key (`PitchPipeButton`) uses a new outlined `key_button_background.xml` with blue stroke/text/icon; Rate is a blue icon button; all 48dp.
- `res/layout/mediaplayerview.xml`: filled play/pause, outlined stop; sliders and part radios take the blue primary.
- FABs on Home/Browse/Search: primary blue with white icon.
- `StatusIndicatorView.kt`: green check when available, secondary-color X when unavailable; label text back to the normal on-surface color.
- `res/layout/meviewheader.xml`: home actions at `titleLarge` medium with blue icons; remaining footer links blue.
- Tests: `BrandStyleRegressionTest.kt` (new) plus updated `PolishLayoutRegressionTest` expectations; an IME search test gained an explicit wait for the results screen.

## Handwriting title

Checked at font scale 1.0, 1.3, 1.5 and 2.0 on Home, Browse, Settings and the Sheet Music screen (crops under `.impeccable/review/tagmaster-android-round3-title-*-crop.png`). No ascender or descender is cut at any scale; no code change was needed on Android.

## Verification (coordinator, final tree)

| Command | Result |
| --- | --- |
| `./gradlew :TagMaster:assembleDebug :TagMaster:testDebugUnitTest :PitchPerfect:assembleDebug` | exit 0; 267 unit tests, 0 failures/errors/skipped (`/tmp/tm-r3-android-verify.log`) |
| `./gradlew :TagMaster:connectedDebugAndroidTest` (emulator-5554, Android 16) | exit 0; 105 tests, 0 failures (`/tmp/tm-r3-connected.log`) |
| `git -c core.whitespace=cr-at-eol diff --check` | clean |

App data was backed up before the connected run and restored after it. Device left at night mode ON, font scale 1.0.

## Captures

Agent: `.impeccable/review/tagmaster-android-round3-{home,browse,search,summary,tracks,sheet,settings}[-dark].png`, `home`/`summary` at 1.3 (`-large`), `popup-dark`, and the 32 title crops. Coordinator, from the final build: `tagmaster-android-round3-verify-{home,home-dark,summary,summary-dark,tracks,popup}.png` (Summary reached through Home > Open Tag > 1809). The status-bar inset is charcoal on every screen in both appearances; the overflow menu is readable on the light surface.

## Left

Nothing in this round's scope. Dynamic color stays off by product identity.
