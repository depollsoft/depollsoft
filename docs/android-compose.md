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

## Tests

* Behaviour: Robolectric + `createAndroidComposeRule` in each app's `src/test`.
* Pixels: Roborazzi screenshot tests (`*ScreenshotTest`) under `src/test`, goldens in
  `src/test/screenshots`. `./gradlew :<App>:recordRoborazziDebug` rewrites goldens,
  `:<App>:verifyRoborazziDebug` fails on a difference and `:<App>:compareRoborazziDebug` writes
  `*_compare.png` diff images next to them. The goldens were first recorded from the View
  implementation, and each Compose screen was diffed against them during the port.
