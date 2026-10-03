# Usage analytics and review prompts

Pitch Perfect and Tag Master report the same screens and events on Android and iOS, and ask for a
store review under the same rules. This page is the shared list; change it together with the code on
both platforms.

## Usage analytics

Events go to Google Analytics through Firebase, and only when the person turned on usage analytics in
Privacy choices (see [privacy-consent.md](privacy-consent.md)); until then Firebase collection is off
and drops them. Every value is a short fixed word or number. Nothing a person typed or named (song
titles, list names, search text, tag titles) is sent.

The code lives in `UsageAnalytics` on each platform:
`Android/depollsoft.lib.kotlin/src/main/java/depollsoft/lib/analytics/UsageAnalytics.kt` and
`iOS/shared/UsageAnalytics.swift`. Each app points it at Firebase at launch, so tests and previews send
nothing. Firebase's automatic screen reporting is off in both apps
(`google_analytics_automatic_screen_reporting_enabled` in the Android manifests,
`FirebaseAutomaticScreenReportingEnabled` in the iOS plists): it named screens after Android activities
and SwiftUI hosting controllers, which lumped every Pitch Perfect tab together and meant nothing on iOS.
Nothing goes to DepollSoft's own analytics endpoint (`api/`) any more: Android's `app_open` event
there was dropped, since Google Analytics records sessions.

### Screens

A `screen_view` carries the same name as both `screen_name` and `screen_class`. It is logged each time
the screen comes to the front: opening it, switching to its tab or page, returning to it from another
screen, or bringing the app back from the background. Screens report through `ScreenView(name)`
(DepollSoftCompose) on Android and `.analyticsScreen(name)` (`iOS/shared/UsageAnalytics.swift`) on iOS.

| App | `screen_name` | Screen |
| --- | --- | --- |
| Pitch Perfect | `pitch_pipe` | Pitch Pipe tab (radial or classic; see `pitch_pipe_style`) |
| Pitch Perfect | `notes` | Notes tab |
| Pitch Perfect | `keys` | Keys tab |
| Pitch Perfect | `songs` | Songs tab |
| Pitch Perfect | `set_lists` | Set lists (manage) |
| Pitch Perfect | `song_editor` | Adding or editing a song |
| Pitch Perfect | `add_songs` | Adding songs from other set lists |
| Pitch Perfect | `settings` | Settings, including the sound list (a dialog on Android, a pushed list on iOS) |
| Tag Master | `home` | Home |
| Tag Master | `browse` | Browse |
| Tag Master | `search` | Search form |
| Tag Master | `search_results` | Search results |
| Tag Master | `tag_list` | One of the person's own lists |
| Tag Master | `teachable_tags` | Teachable tags |
| Tag Master | `tag_summary`, `tag_details`, `tag_tracks`, `tag_videos` | The four pages of a tag, phone or side pane, from the first load on; again for each new tag in a reused side pane |
| Tag Master | `sheet_music` | Sheet music |
| Tag Master | `settings` | Settings |

Dialogs, alerts, menus and system or SDK screens (sign-in, privacy, ads, share) are not screens here.

### Events

| Event | App | Parameters | Logged when |
| --- | --- | --- | --- |
| `pitch_played` | both | `source` | Someone starts a note: a press or tap (or a screen-reader activation), not sliding onto a neighbour, stopping a note, or Settings' sound preview |
| `song_added` | Pitch Perfect | | A new song is saved (not an edit) |
| `set_list_created` | Pitch Perfect | | A new set list is named (not a rename or duplicate) |
| `songs_added_to_set_list` | Pitch Perfect | | Songs from other set lists are added |
| `tag_viewed` | Tag Master | | A tag's details finish loading (not a refresh of the same tag) |
| `learning_track_played` | Tag Master | `part` | A learning track starts (or starts over) after it loads; resuming from a pause doesn't count |
| `video_opened` | Tag Master | | A teaching or performance video opens |
| `tag_added_to_list` | Tag Master | `list` | Someone adds a tag to a list (not undo, sync or migration) |
| `tag_list_created` | Tag Master | | Someone creates a list |
| `login` | both | `method` | Sign-in succeeds |
| `review_prompt_requested` | both | | The app asks the store for a review (see below); the store may show nothing |

Parameter values:

- `source` (Pitch Perfect): `pitch_pipe` (radial face), `classic_pitch_pipe`, `notes`, `keys`, `song`,
  `widget`. (Tag Master): `tag` (the key note on a tag's summary), `sheet_music` (the key note on the
  sheet music screen, where there is one).
- `part`: `all`, `tenor`, `lead`, `baritone`, `bass`, `other`.
- `list`: `favorites`, `teachable`, `custom`.
- `method`: `google`, `apple`, `facebook`, `email`, `phone`, `other`, from the Firebase provider that
  signed in (`google.com`, `apple.com`, `facebook.com`, `password`, `phone`).

### User properties

| Property | App | Values | Set |
| --- | --- | --- | --- |
| `pitch_pipe_style` | Pitch Perfect | `radial`, `classic` | At launch and when changed |
| `note_sound` | Pitch Perfect | the sound's synced id (`pitchPipe`, `piano`, ...) | At launch and when changed |
| `reference_pitch` | Pitch Perfect | A4 in Hz (`440`) | At launch and when changed |
| `signed_in` | both | `yes`, `no` | At launch and on sign-in or sign-out |

User properties sent while collection is off are dropped, so each app sends them again when someone
turns usage analytics on (usually on the first launch's Privacy choices).

## Review prompts

Both apps use the store's own review card (Google Play In-App Review, `AppStore.requestReview(in:)`),
which the stores also rate-limit. The rules below decide when the app asks at all. The goal is that the
card never gets in the way of someone using the app: not while they find a pitch, sing a tag, read sheet
music or rehearse.

The code lives in `ReviewPolicy`/`ReviewPrompt`
(`Android/depollsoft.lib.kotlin/src/main/java/depollsoft/lib/review/`, `iOS/shared/ReviewPrompt.swift`).
Its counts stay on the device, are never sent anywhere, and do not depend on the analytics choice.

### Who may be asked

- They have used the app on at least **5 different days**, and the first of those was at least
  **14 days** ago. A day of use is a day with the app's main job done: a pitch played (Pitch Perfect),
  a tag opened (Tag Master).
- They have not been asked in this app version, nor in the last **180 days**. After an ask the days of
  use start over.
- Nothing has sounded for **5 minutes**: no pitch, key note or learning track has started or stopped,
  and no video was opened. The quiet runs from the end of a sound, so a track played for ten minutes
  still counts as recent when it stops. Someone who just played something may be rehearsing or singing
  with others.

### When

All of these, in order:

1. Someone **finishes a task away from the music**:
   - Pitch Perfect: saves a new song, creates a set list, or adds songs from other set lists.
   - Tag Master: adds a tag to a list (favorites, teachable or their own), or creates a list.
2. Within **2 minutes**, a **calm screen** is in front. A calm screen is one nobody reads or plays from
   while singing:
   - Pitch Perfect: the Songs tab, with no sheet over it.
   - Tag Master: Home, with no tag beside it on a tablet.

   A pitch pipe, a tag's pages and sheet music are never calm.
3. The screen then stays **untouched for 3 seconds**: no touch, key press or pointer movement.
4. It is still in front with nothing over it (no dialog, sheet, menu, keyboard or text field in use).
   The app is foreground and active, and nothing is sounding.

A touch during the 3 seconds spends that task's chance, and so does leaving the app (on iOS, even
briefly, as for Control Center), so nothing is asked on coming back. The next chance comes with the next
finished task. Nothing is asked at launch.

Debug builds never call the store (both log a line instead), and neither do tests: each
platform's tests replace the store call.

### Hooks

| | Android | iOS |
| --- | --- | --- |
| Start counting (launch) | `ReviewPrompt.install(context)` | `ReviewPrompt.shared.install()` |
| Day of use | `ReviewPrompt.recordUse()` | `ReviewPrompt.shared.recordUse()` |
| Sound | `ReviewPrompt.recordSound()` | `ReviewPrompt.shared.recordSound()` |
| Finished task | `ReviewPrompt.taskFinished()` | `ReviewPrompt.shared.taskFinished()` |
| Calm screen | `ReviewCalmScreen(calm)` (DepollSoftCompose) | `.reviewCalmScreen(isCalm)` |
| Sounding now | `ReviewPrompt.isBusy` | `ReviewPrompt.shared.isBusy` |

Each app's own calls are in one place: `PitchPerfectAnalytics`/`TagMasterAnalytics` on Android and
`PitchPerfectUsage`/`TagMasterUsage` on iOS.

### Tests

- Rules and timing: `ReviewPolicyTest`, `ReviewPromptTest` and `UsageAnalyticsTest`
  (depollsoft.lib.kotlin); `ReviewPromptTests` (Tag Master's iOS test bundle covers the shared iOS code).
- Flows through the real screens: `UsageAndReviewTest`, `PitchPlayedTest` and `TrackUsageTest` on
  Android; `PitchPerfectAnalyticsTests` and `TagMasterAnalyticsTests` on iOS.
- Not covered by tests: delivery to Firebase (check DebugView on a device that opted in), and the store
  card itself, which debug builds never request.
