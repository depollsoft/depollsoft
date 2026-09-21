# Tag Master: user-defined tag lists

Tag Master keeps tags in lists. Two are built in and always present — **Favorites** and **Teachable
Tags** — and the user can create any number of their own ("Afterglow set", "Chorus warmups"). This
page covers the data model the two platforms share, and how to run the emulator-backed sync tests
that prove they agree.

## Data model

Everything lives in the signed-in user's Firestore document, `users/{uid}`:

```
lists:    { favorite: [int], teachable: [int], "<key>": [int], ... }
listInfo: { "<key>": { name: "Afterglow set", order: 0 }, ... }
```

- `lists` is the existing map of tag ids. Any key that is not `favorite` or `teachable` is a
  user-created list.
- `listInfo` holds only custom lists. The built-in two never appear in it.
- A key is a stable slug generated once from the name plus a four-character suffix
  (`afterglow-set-k3f9`). Renaming changes `listInfo.<key>.name` and nothing else, so tags never
  move and an edit made offline on another device still lands in the right list.
- Custom lists sort by `listInfo.<key>.order` ascending, then by name.
- A key present in `lists` with no `listInfo` entry is a list written by an older app version. It is
  shown under its raw key rather than hidden.
- Empty lists are legal. Android deletes the `lists` field when a list empties and keeps the
  `listInfo` entry.
- Writes are per field (`SetOptions.mergeFieldPaths` / `mergeFields`), so an old app version that
  knows nothing about `listInfo` cannot clobber a custom list.

Names are trimmed and whitespace-collapsed, capped at 60 characters, and rejected when they
duplicate another list (case-insensitively) or match a built-in name.

**Signing in.** The device remembers which account last synced its lists (`tagmaster.syncedUid`
/ `depollsoft.tagmaster.syncedUid`). Signing in as a *different* account drops the local lists
first, so one user's lists are never uploaded into another user's brand-new document; signing out
keeps them, and the same account signing back in finds them untouched. Then the account's document
is the truth. Its lists replace whatever the device had,
including the built-in ones (a built-in list the document does not mention is empty), so a device's
local-only lists never leak into an existing account. The one time the device's lists are uploaded
is when the *server* confirms the account has no document yet, which is a brand-new account. A
snapshot served from the local cache (an offline start with a cache miss) decides nothing: it
neither seeds the document nor clears the local lists. Both listeners subscribe with metadata
changes included so that server confirmation always arrives (`ListModel.applyUserSnapshot`,
`DPAppDelegate.handleUserSnapshot`).

Locally the same data is cached per platform:

| | Android (`Preferences`) | iOS (`UserDefaults`) |
|---|---|---|
| tag ids | `tagmaster.lists` | `depollsoft.pitchperfect.lists` |
| names | `tagmaster.listNames` | `depollsoft.tagmaster.listInfo` |
| order | `tagmaster.listOrder` | (same key, `order` field) |

The entry points are `TagLists` / `ListModel` on Android
(`Android/TagMaster/src/main/java/depollsoft/tagmaster/`) and `TMTagLists` on iOS
(`iOS/tagmaster/tagmaster/TMTagLists.swift`). Everything else goes through them, including the
legacy `DPAppDelegate.favorites()` family and `FavoritesModel` / `TeachableTagsModel`.

## Running the emulators

The sync tests talk to the Firestore and Auth emulators, never to a real Firebase project:

```bash
scripts/firestore-emulator.sh tagmaster
```

Firestore listens on `localhost:8080`, Auth on `localhost:9099`; the ports and the disabled
emulator UI are configured in `Firebase/tagmaster/firebase.json`. The script pins
`JAVA_HOME=/opt/homebrew/opt/openjdk@21` because firebase-tools needs a JDK 21 or newer and the
shell profile here points at 17, and it always runs under the project id `demo-tagmaster`. That id
is reserved for emulation: the emulators refuse to reach any real Google service under it, so no
run can touch production data even with live credentials on the machine.

Stop the emulators with Ctrl-C. Nothing persists between runs.

## Running the sync tests

Both suites skip themselves — they do not fail — when nothing is listening on port 8080, so an
ordinary test run needs no emulator.

**Android** (`Android/TagMaster/src/test/kotlin/depollsoft/tagmaster/TagListSyncEmulatorTest.kt`,
Robolectric, on the JVM):

```bash
cd Android
./gradlew :TagMaster:testDebugUnitTest --tests 'depollsoft.tagmaster.TagListSyncEmulatorTest'
```

Run that class **on its own**, as above. The Robolectric screen suites refuse every outbound
http(s) request for the whole test JVM by installing a `URLStreamHandlerFactory`
(`ScreenTestSupport.blockNetwork`), and that hook can only be installed once per JVM and never
removed. Firebase Auth reaches the emulator through `java.net.URL`, so in a JVM where a screen test
has already run it cannot sign in. The class detects that and skips, which is why a whole-module
`./gradlew :TagMaster:test` run reports it as skipped even with the emulators up.

**iOS** (`iOS/tagmaster/tagmasterTests/TMListSyncEmulatorTests.swift`):

```bash
cd iOS
xcodebuild test -workspace iOS.xcworkspace -scheme tagmaster \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -only-testing:tagmasterTests/TMListSyncEmulatorTests
```

Each suite creates a throwaway email/password account in the Auth emulator and signs **two**
clients in as it: the app's own client, driven through the real listener
(`ListModel.connectToFirestore()` / `DPAppDelegate.connectLists(to:)`), and a second Firebase app
standing in for the user's other device. Two clients rather than one because the security rules
(`request.auth.uid == uid`) only let a client read and write its own document, and because a
second client is the only way to produce a change the app under test did not make itself. The tests
then assert both directions: local creates, renames, reorders and deletes produce exactly the
document shape above, and a write from the other device updates the local registry, list names and
tag ids.

Neither suite may call `FirebaseApp.initializeApp(context)` or `FirebaseApp.configure()` without
options: those read the real `google-services.json` / `GoogleService-Info.plist`, and a Robolectric
run once uploaded a crash to the *production* Crashlytics project that way. Both build their apps
from synthetic options instead. The ids and keys are shaped like real ones only because Firebase
Installations validates their format and takes Firestore's auth provider down with it otherwise.

The registry itself is covered without any emulator by `TagListsTest.kt` (Android) and
`TMTagListsTests.swift` (iOS): naming rules, key generation, ordering, legacy keys, the cloud
payload shape and the local persistence round trip.
