# Android UI: Jetpack Compose

Pitch Perfect, Pitch Perfect for Wear OS and Tag Master draw their UI with Jetpack Compose. The
Bindroid data-binding library they used to share is gone; this page describes what replaced it
and the conventions the screens follow.

## State

Models keep observable state in Compose snapshot state, through the helpers in
`DepollSoftCommon/src/main/java/depollsoft/lib/state`:

| Bindroid                        | Now                                                        |
| ------------------------------- | ---------------------------------------------------------- |
| `TrackableField<T>`             | `StateField<T>` (Java) or `var x by mutableStateOf(...)`   |
| `TrackableCollection<T>`        | `StateList<T>` (a `SnapshotStateList` with a public no-arg constructor) |
| `collection.transaction { }`    | `stateList.transaction { }` / `batchStateChanges { }`      |
| raw `Trackable` + `track()`/`updateTrackers()` | `ChangeSignal.read()` / `ChangeSignal.changed()` |
| `track({ read }) { onChange }` / `Trackable.track(tracker, fn)` | `watchState(read = { ... }) { value -> ... }` |
| `UiBinder.bind(...)`, converters, `BoundUi` views | composables reading the model directly |

Composables read model state directly and recompose when it changes. Non-UI code that must react
to a change (persisting a list, syncing to Firestore) prefers an explicit call at the mutation
site; `watchState` is for the cases where the writer cannot know who cares.

Two behaviours differ from Bindroid:

* Bindroid notified trackers synchronously inside `set()`. Snapshot state notifies observers when
  the global snapshot sends apply notifications, which `SnapshotNotifications` schedules on the
  next main-looper turn. Tests call `SnapshotNotifications.flush()` and idle the main looper.
* A state object reports writes only after `Snapshot.notifyObjectsInitialized()`; the Recomposer
  and `watchState` both call it, so this only matters to code observing snapshot state by hand.

### Stored data

`Preferences` persists values through `JsonSerializer`, which writes each collection's class under
its registered alias. Lists stored by Bindroid-era versions carry the aliases `List` (Pitch
Perfect) and `depollsoft.lib.binding.ObservableCollection` (Tag Master); each app registers
`StateList` under its alias, so those lists load unchanged. Tests in each app load fixtures
serialized by the Bindroid-era code to keep that true.

## Screens

* One activity per existing entry point; each calls `setContent { AppTheme { Screen(...) } }`.
  Fragments, `ViewPager2`, `RecyclerView` adapters and XML layouts are gone.
* Screen composables take their model and callbacks as parameters so tests and previews can
  drive them without an activity.
* Custom-drawn surfaces (the pitch instrument faces, the barber-pole loader, the score
  background) keep their drawing code in a renderer that paints onto an `android.graphics.Canvas`,
  called from a Compose `Canvas` through `drawIntoCanvas { it.nativeCanvas }`. The pixels are
  therefore the ones the Views drew.
* Platform views with no Compose equivalent (the AdMob banner, video playback) are hosted with
  `AndroidView`. The home-screen widget stays on `RemoteViews`.
* Stable `Modifier.testTag`s identify controls; `testTagsAsResourceId` is on so UiAutomator-based
  store captures can still find them.
* State that a View kept across recreation (an open DialogFragment, an EditText's text, a retained
  fragment's loaded pages) is kept too: in `rememberSaveable`, the activity's `SavedStateRegistry`,
  or a `ViewModel`.

## Shared Compose code

`DepollSoftCompose` (package `depollsoft.compose`) holds what both apps' screens use. It depends
on Compose UI and foundation only; each app keeps its own Material version and draws the visible
parts (the snackbar, the tooltip) itself:

* `ListMotion` and `listItemMotion`: how list rows move.
* `ReorderState`, `reorderHandle`, `reorderRow`, `shownOrder`: drag-to-reorder.
* `listViewScrollbar`, `recyclerScrollbar`, `scrollViewScrollbar`, `revealItem`: View scrollbars
  and smooth scrolling to a row.
* `SnackbarState`, `SlidingSnackbarHost`, `SnackbarTiming`: one snackbar at a time, MDC's timing.
* `LabelTooltipState`, `tooltipOnHover`: icon labels on long press and mouse hover.
* `MenuKey`, `OpensOnMenuKey`: the hardware Menu key.
* `inWholePixels`, `viewPx`, `viewDp`, `ViewAlign`, `dialogTitleFits`, `dialogWindowWidth`,
  `rememberDrawable`, `drawPlatform`: the pixel rules below.

Anything both apps need goes there rather than into a second copy: the two copies this module
replaced had already drifted apart (one app got a fix the other did not).

## Motion and interaction

Both apps move the same way. Keep new screens consistent with this:

* **List rows** that can be added, removed, moved or changed use `listItemMotion`
  (RecyclerView's default animator: 120ms fades, 250ms moves). A list that can be reordered
  shows `shownOrder`, which holds the scroll position by index whenever the rows only trade
  places (a drag, Sort, Move up/down, a sync), so the list doesn't follow its old top row.
* **Dragged rows** (`ReorderState`) start on touch-down of the handle, lift to a 6dp shadow in
  150ms, trade places once they pass a neighbour's far edge, and settle into their slot in 200ms.
  The new order is committed once, on the drop, and only if it changed. A refused drop or a drag
  abandoned by `sourceChanged` settles the row the same way. The haptics are Android 14's gesture
  feedback, and a single long-press buzz at the start on older versions.
* **Snackbars** last 1.5s (short) or 2.75s (long) once they have slid in, as MDC's did,
  stretched to the accessibility timeout the user asked for. A new one waits for the one it
  replaces to slide away. Screen readers hear them (a polite live region).
* **Icon buttons and tabs** show their label as a tooltip on long-press and mouse hover, as
  AppCompat's did. The long press buzzes once: `combinedClickable`'s buzz. The hardware Menu key
  opens the screen's overflow menu.
* **Dialogs and popup menus** animate in and out. Pagers select a tapped tab at once and a swiped
  one when it settles; side effects of "the current page" (stopping a track) wait for
  `settledPage`.
* Compose `clickable` plays the system click sound. A custom `pointerInput` gesture that stands
  for a click has to call `playSoundEffect` itself.

## Matching the Views' pixels

The screens reproduce the View layouts they replaced, down to the pixel. What that took, and what
new screens and changes need to keep doing:

* **Text sizes are whole pixels.** A TextView reads a size from XML or a text appearance with
  `getDimensionPixelSize`, so 14sp at 2.625x is 37px, not 36.75px. The type scales
  (`TagMasterType`, `plateText`, `LegacyText`) round the same way. Sizes the View code set with
  `setTextSize`, MDC's chip text and `TextInputLayout`'s floating label keep the fraction, and so
  do their Compose counterparts.
* **View code truncated dp.** Custom Views computed `(dp * density).toInt()`; where they did, the
  Compose layouts use `viewPx` (or `viewDp`) rather than `roundToPx()`.
* **Centring** uses the View rule (an odd leftover pixel goes below or after); see `ViewAlign`,
  whose start and end alignments mirror in a right-to-left layout.
* **Dialog titles** follow AppCompat's DialogTitle: a wrap-content dialog window is first measured
  at the platform's preferred width, 320dp, and a title that would ellipsize there switches to
  18sp over two lines for good, even if it fits the dialog that opens.
* Whole-number densities (xhdpi, xxhdpi) hide rounding differences, since every rule agrees there.
  Keep at least a few goldens at a fractional density such as 420dpi.

## Tests

* Behaviour: Robolectric with the Compose test APIs (the v2 rules in
  `androidx.compose.ui.test.junit4.v2`) in each app's `src/test`.
* Pixels: Roborazzi screenshot tests (`*ScreenshotTest`) under `src/test`, goldens in
  `src/test/screenshots`, most at xxhdpi and xhdpi and a few (`dpi420_*`) at 420dpi.
  `./gradlew :<App>:recordRoborazziDebug` rewrites goldens, `:<App>:verifyRoborazziDebug` fails on
  a difference and `:<App>:compareRoborazziDebug` writes `*_compare.png` diff images next to them.
  Roborazzi's comparison tolerates small differences by default, so judge an intended pixel-exact
  change by comparing the PNGs themselves. The goldens were first recorded from the View
  implementation, and each Compose screen was diffed against them during the port. CI runs
  `verifyRoborazziDebug` for every selected app and uploads the actual and comparison images when a
  golden no longer matches. The screenshot setups pin the version name the about footers show, so
  a release doesn't change the goldens.
* Pitfalls:
  * An infinite `withFrameMillis` loop never lets the test clock go idle; use
    `withInfiniteAnimationFrameMillis`, which tests park, and test the animation's maths directly.
  * `performClick()` injects a real tap; a screen reader's activation is
    `performSemanticsAction(SemanticsActions.OnClick)`.
  * In an instrumented test the rule owns the frame clock and advances it only when the test
    synchronizes, so a loop polling model state must call `mainClock.advanceTimeBy` for animations
    to run. Semantics queries must not run on the main thread.
  * Test tags on nodes merged into a parent need `useUnmergedTree = true`.
* Device-only suites (`src/androidTest`: store captures, drags, privacy consent, PDF rendering)
  are compiled by CI but not run; run them with `./gradlew :<App>:connectedDebugAndroidTest` on an
  emulator, and the store captures with
  `-Pandroid.testInstrumentationRunnerArguments.storeScreenshots=true`. Tag Master's suites that
  change saved lists use `SavedListsSandbox`, which refuses to run while signed in and restores the
  device's lists afterwards.
