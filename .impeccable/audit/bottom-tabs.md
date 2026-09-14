# Restore bottom page tabs

User decision: prioritize familiar, one-handed page switching over the earlier recommendation to move tabs to the top.

## Changes

- `Android/TagMaster/src/main/res/layout/tagmasterview.xml`: Browse tabs follow the weighted content region; Search remains above the strip.
- `Android/TagMaster/src/main/res/layout/tagdetailview.xml`: Summary, Details, Tracks and Videos follow the content region. Loading/error behavior and hidden-until-loaded tabs are unchanged.
- Retained charcoal backgrounds, blue selection, icons, labels, tab order and ViewPager2 bindings. The indicator now sits on the top edge of the bottom strip. Auto sizing distributes tabs when they fit and permits scrolling when they do not.
- Existing root-owned system-bar insets remain unchanged. No added padding or overlays can double-count the safe area.
- iOS already has bottom page tabs; no iOS changes were needed.

## Acceptance checks

- New `BottomTabsLayoutTest`: 3/3 pass for phone, landscape, expanded/dark. Both layouts put tabs last, keep the pager and Search FAB above them, preserve Detail's initial visibility, and leave at least 48dp touch height above the simulated bottom system inset.
- `:TagMaster:assembleDebug`: passed.
- Existing device tests `MeActivityTest`, `TagDetailActivityTest`, `PolishFlowRegressionTest`: 57/57 passed via AndroidJUnitRunner. Covers tab taps, swipes, repeated switching, restored selection, navigation, and landscape track selection.
- Targeted LSP diagnostics: no findings on the new test file.
- Native captures inspected for Browse light, Detail light/dark, Detail at font scale 1.3, landscape Detail, and expanded Browse. Pager bounds end exactly where tabs begin; phone tabs end at y=2337 above the gesture area on a 1080x2400 emulator. Search is above the bottom strip.
- Device restored to night mode on, font scale 1.0, auto rotation, physical 1080x2400 and density 420. Private data was backed up and restored around the native test run.

## Commands and evidence

From `Android/`, JDK 17:

```sh
./gradlew :TagMaster:testDebugUnitTest --tests depollsoft.tagmaster.BottomTabsLayoutTest :TagMaster:assembleDebug
./gradlew :TagMaster:assembleDebugAndroidTest
adb -s emulator-5554 shell am instrument -w -r -e class depollsoft.tagmaster.MeActivityTest,depollsoft.tagmaster.TagDetailActivityTest,depollsoft.tagmaster.PolishFlowRegressionTest depollsoft.tagmaster.test/androidx.test.runner.AndroidJUnitRunner
```

Logs: `/tmp/tm-bottom-tabs-unit.log`, `/tmp/tm-bottom-tabs-native.log`. Captures: `.impeccable/review/tagmaster-android-bottom-tabs-*.png`. Expanded evidence uses an emulator size/density override, not tablet hardware. No unrelated suites were repeated for this layout-only change.
