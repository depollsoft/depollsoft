# App test layout (iOS and Android)

Both apps test screen behaviour in-process, on the JVM or in a hosted XCTest
bundle, and keep only a small device-only residue as UI/instrumented tests.
This keeps every pull request check on unit tests and moves the residue to
scheduled or on-demand runs.

## iOS

| Bundle | Runs on | What it holds |
|--------|---------|---------------|
| `pitchperfectTests`, `tagmasterTests` | every PR (`iOS CI`) | Real view controllers mounted in a test `UIWindow` from the production storyboards, driven through the same actions users trigger. Fixtures seed data through the app's own caches (`DPTag` cache seam, `DPSongList` setter); nothing touches the network. |
| `pitchperfectlibTests`, `depolllibTests` | every PR | Library and model tests. |
| `pitchperfectUITests`, `tagmasterUITests` | weekly `iOS CI` (`extended-ui`), `workflow_dispatch`, release capture | XCUITest residue: launch metrics, software keyboard, rotation hit targets in compact height, system sheets (share, FirebaseUI sign-in, notification permission), WidgetKit, and `StoreScreenshotTests`. `scripts/release/capture.py` runs `StoreScreenshotTests` for App Store assets, so these targets must stay. |

Suite selection lives in `scripts/ci/ios_suites.py` (`SUITES`): the `-ui` suites
are only included with `--extended`.

Run locally from `iOS/`:

```bash
# Unit suites (what PRs run)
xcodebuild test -workspace iOS.xcworkspace -scheme tagmaster \
  -only-testing:tagmasterTests -destination 'platform=iOS Simulator,name=iPhone SE (3rd generation)'
xcodebuild test -workspace iOS.xcworkspace -scheme pitchperfect \
  -only-testing:pitchperfectTests -destination 'platform=iOS Simulator,name=iPhone SE (3rd generation)'

# UI residue
xcodebuild test -workspace iOS.xcworkspace -scheme tagmaster -only-testing:tagmasterUITests ...
xcodebuild test -workspace iOS.xcworkspace -scheme pitchperfectUITests ...
```

Guidelines for hosted tests: drive appearance with
`beginAppearanceTransition`/`endAppearanceTransition` when a controller relies
on `viewDidAppear`; spin the run loop until the condition holds instead of
using fixed sleeps or `XCTNSPredicateExpectation` (which polls once a second);
pre-cache every tag or song a screen will load and evict fixtures after the
window is torn down.

## Android

| Source set | Runs on | What it holds |
|------------|---------|---------------|
| `src/test` (Robolectric) | every PR (`Android CI`, `testDebugUnitTest`) | Screen tests with `ActivityScenario` and Espresso matchers on the JVM, plus ViewModel/binding tests. HTTP is blocked at the URL boundary and tag fixtures are installed through the `Tag` cache and `TagDetailActivity.tagLoader` seam. |
| `src/androidTest` | no CI runner; device or emulator by hand | Residue only: native pointer drags with `ItemTouchHelper`, IME window geometry, `PdfRenderer`, store-screenshot capture (opt-in instrumentation arguments), and the FirebaseUI null-email bytecode-patch check, which only exists in the packaged app. |

Run locally from `Android/`:

```bash
./gradlew :TagMaster:testDebugUnitTest :PitchPerfect:testDebugUnitTest
./gradlew :TagMaster:connectedDebugAndroidTest   # residue, needs a device
```

Guidelines for Robolectric screen tests: clear both the in-memory and disk tag
caches around each test, await the loaded state by draining the main looper
with a bounded deadline rather than a single `idle()`, and seed `Preferences`
defaults explicitly so tests do not depend on order.
