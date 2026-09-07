# Tag Master PDF chart fix

Root verified first with `pwd`: `/Users/depoll/.local/share/pi-worktrees/20260907005637/tm-refine-worktree-20260907`.

## Exact fix list for reviewer

1. `Android/TagMaster/src/main/java/depollsoft/tagmaster/SheetMusicActivity.kt:142-143`: rename loop variable to `pageIndex`; call `renderer.openPage(pageIndex)` instead of `renderer.openPage(0)`. Only these two production lines changed relative to the starting worktree. Existing `page.close()` and `renderer.close()` remain unchanged, including their existing exception-path behavior.
2. `Android/TagMaster/src/androidTest/java/depollsoft/tagmaster/SheetMusicPdfTest.kt`: add one native instrumentation regression test. Generate a two-page PDF locally with red Page 1 and blue Page 2. Seed the normal tag disk cache and launch the actual `SheetMusicActivity`, without catalog access, mocks, renderer extraction or test-only production code. Clean up the PDF and disk tag fixture.
3. This evidence report. No UI changes, configuration edits, other fixes, commits or delegation.

## Acceptance results

- PASS: actual production PhotoView bitmap is 400×800, containing two square pages stacked vertically.
- PASS: first page center is exactly red; second page center is exactly blue; pixels differ and original order is retained.
- PASS: no file descriptor in `/proc/self/fd` points to the fixture PDF after rendering. Opening the second native page and completing rendering also exercises the retained per-page close path.
- PASS: one instrumentation test on `emulator-5554`, API36, `OK (1 test)`, 1.389 seconds. This is real Android PdfRenderer, not Robolectric. Existing sdk35/TestDeskApplication unit-test configuration was not changed or used.
- PASS: inspected actual viewer screenshot through Pi read. Dark large-text viewer shows red Page 1 above blue Page 2. Preview is 360×800, 15,418 bytes.
- PASS: starting-file comparison shows exactly the two-line production fix; targeted `git diff --check` passed. Pi's edit-time Kotlin diagnostics reported clean.

## Commands and complete logs

All artifacts remain under `Android/TagMaster/build/chart-fix-evidence/`, within owned scope. These are local build artifacts and can be removed by Gradle clean.

Environment:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_SERIAL=emulator-5554
ADB="$ANDROID_HOME/platform-tools/adb"
cd Android
```

Initial connected-run attempt:

```sh
./gradlew --offline :TagMaster:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=depollsoft.tagmaster.SheetMusicPdfTest --console=plain
```

`connected-test.log`: stopped before compilation because UTP `android-test-plugin-host-additional-test-output:32.3.2` was not cached offline. No network fetch attempted. Switched to direct ADB instrumentation without changing project configuration.

```sh
./gradlew --offline :TagMaster:assembleDebug :TagMaster:assembleDebugAndroidTest --console=plain
```

`assemble.log`: first compilation caught the test fixture using `use` on PdfDocument, which does not implement Closeable. Corrected fixture to explicit try/finally with `document.close()`.

After that final source edit, one focused build/test sequence:

```sh
./gradlew --offline :TagMaster:assembleDebug :TagMaster:assembleDebugAndroidTest --console=plain
$ADB -s emulator-5554 install -r TagMaster/build/outputs/apk/debug/TagMaster-debug.apk
$ADB -s emulator-5554 install -r TagMaster/build/outputs/apk/androidTest/debug/TagMaster-debug-androidTest.apk
$ADB -s emulator-5554 shell am instrument -w -r -e class depollsoft.tagmaster.SheetMusicPdfTest depollsoft.tagmaster.test/androidx.test.runner.AndroidJUnitRunner
```

`assemble-final.log`: BUILD SUCCESSFUL in 6s. `install.log`: both installs succeeded. `instrumentation.log`: one test passed, status code 0, final instrumentation code -1. No passing test was rerun.

Evidence collection from repository root:

```sh
E=Android/TagMaster/build/chart-fix-evidence
$ADB -s emulator-5554 exec-out run-as depollsoft.tagmaster cat cache/chart-two-pages-preview.png > "$E/chart-two-pages-preview.png"
$ADB -s emulator-5554 logcat -d > "$E/logcat.log"
diff -u "$E/SheetMusicActivity.before.kt" Android/TagMaster/src/main/java/depollsoft/tagmaster/SheetMusicActivity.kt > "$E/production.patch"
sips -g pixelWidth -g pixelHeight "$E/chart-two-pages-preview.png"
wc -c "$E/chart-two-pages-preview.png"
git diff --check -- Android/TagMaster/src/main/java/depollsoft/tagmaster/SheetMusicActivity.kt Android/TagMaster/src/androidTest/java/depollsoft/tagmaster/SheetMusicPdfTest.kt
```

`logcat.log` retains the complete collected device log, including `PASS: 400x800, first=red, second=blue, PDF descriptors=0`. `production.patch` and `SheetMusicActivity.before.kt` preserve the bounded before/after evidence separately from pre-existing worktree changes. The generated PDF itself is deleted by test cleanup; the actual viewer preview is retained. No broader audit or suite was run.
