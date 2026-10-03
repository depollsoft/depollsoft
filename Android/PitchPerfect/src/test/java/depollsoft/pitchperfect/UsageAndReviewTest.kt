package depollsoft.pitchperfect

import android.app.Activity
import android.content.Context
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.analytics.UsageAnalytics
import depollsoft.lib.privacy.PrivacyChoices
import depollsoft.lib.review.ReviewPolicy
import depollsoft.lib.review.ReviewPrompt
import depollsoft.pitchperfect.lib.Note
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * What Pitch Perfect reports to analytics as someone moves around and plays (docs/analytics.md), and
 * the one place the review card may follow: the Songs tab, a few quiet seconds after a new song.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class, qualifiers = "w411dp-h891dp")
class UsageAndReviewTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val screens = ComposeScreens(compose)
    private val events = mutableListOf<Pair<String, Map<String, String>>>()
    private val properties = mutableMapOf<String, String>()
    private val asked = mutableListOf<Activity>()
    private val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("usage_review_test", Context.MODE_PRIVATE)
    private var now = 1_790_881_200_000L
    private val policy = ReviewPolicy(prefs, { now })

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        PurchaseService.areAdsRemoved = true
        SongsModel.get().clearAll()
        PrivacyChoices(RuntimeEnvironment.getApplication()).save(analytics = true, crashes = false)
        prefs.edit().clear().commit()
        UsageAnalytics.sink =
            object : UsageAnalytics.Sink {
                override fun logEvent(name: String, params: Map<String, String>) {
                    events += name to params
                }

                override fun setUserProperty(name: String, value: String) {
                    properties[name] = value
                }
            }
        ReviewPrompt.resetForTesting()
        ReviewPrompt.policyForTesting = policy
        ReviewPrompt.showStoreReview = { asked += it }
    }

    @After
    fun tearDown() {
        Note.getCommonNotes().forEach { it.stop() }
        SongsModel.get().clearAll()
        PurchaseService.areAdsRemoved = false
        UsageAnalytics.resetForTesting()
        ReviewPrompt.resetForTesting()
        screens.finish()
    }

    private fun screenViews() = events.filter { it.first == UsageAnalytics.SCREEN_VIEW }.map { it.second.getValue(UsageAnalytics.SCREEN_NAME) }

    private fun named(name: String) = events.filter { it.first == name }

    /** Five days of use over two weeks, and nothing played for a while. */
    private fun becomeEligible() {
        repeat(ReviewPolicy.MIN_ACTIVE_DAYS) {
            policy.recordUse()
            now += DAY
        }
        now += ReviewPolicy.MIN_DAYS_SINCE_FIRST_USE * DAY
    }

    private fun launchMain(): ActivityController<PitchPerfectActivity> =
        screens.launch(PitchPerfectActivity::class.java).also {
            it.windowFocusChanged(true)
            screens.settle()
        }

    /** Adds a song through the editor the Songs tab's add button opens, as the user would. */
    private fun addSong(main: ActivityController<PitchPerfectActivity>, title: String) {
        screens.click(TestTags.ADD_SONG_FAB)
        val started = shadowOf(main.get()).nextStartedActivity
        main.pause()
        val editor = screens.launch(AddSongActivity::class.java, started)
        compose.onNodeWithTag(TestTags.SONG_TITLE).performTextReplacement(title)
        screens.click(TestTags.SAVE_SONG)
        assertTrue("saving closes the editor", editor.get().isFinishing)
        editor.pause().stop().destroy()
        main.resume()
        main.windowFocusChanged(true)
    }

    @Test
    fun eachTabReportsItsOwnScreen() {
        val main = launchMain()
        with(screens) {
            main.get().show(MainTab.NOTES)
            main.get().show(MainTab.KEYS)
            main.get().show(MainTab.SONGS)
            main.get().show(MainTab.PITCH_PIPE)
        }
        assertEquals(listOf("pitch_pipe", "notes", "keys", "songs", "pitch_pipe"), screenViews().distinctConsecutive())
    }

    @Test
    fun comingBackToTheAppReportsTheScreenAgain() {
        val main = launchMain()
        events.clear()
        main.pause().stop()
        main.restart().start().resume()
        screens.settle()
        assertEquals(listOf("pitch_pipe"), screenViews())
    }

    @Test
    fun savingANewSongIsReportedAndTheSongsTabAsksAfterThreeQuietSeconds() {
        becomeEligible()
        val main = launchMain()
        with(screens) { main.get().show(MainTab.SONGS) }
        addSong(main, "Shenandoah")
        assertEquals(1, named("song_added").size)
        assertTrue(screenViews().contains("song_editor"))
        // The editor took a moment to close; the Songs tab then waits for three untouched seconds.
        screens.settle()
        screens.settle()
        assertEquals(listOf<Activity>(main.get()), asked)
        assertEquals(1, named(ReviewPrompt.ASKED_EVENT).size)
    }

    @Test
    fun editingASongIsNotAddingOne() {
        val main = launchMain()
        with(screens) { main.get().show(MainTab.SONGS) }
        addSong(main, "Shenandoah")
        events.clear()
        screens.click(TestTags.EDIT_SONGS)
        screens.click(TestTags.EDIT_SONG_BUTTON)
        val editor = screens.launch(AddSongActivity::class.java, shadowOf(main.get()).nextStartedActivity)
        compose.onNodeWithTag(TestTags.SONG_TITLE).performTextReplacement("Shenandoah (tag)")
        screens.click(TestTags.SAVE_SONG)
        editor.pause().stop().destroy()
        assertEquals(emptyList<Pair<String, Map<String, String>>>(), named("song_added"))
    }

    @Test
    fun noOneIsAskedOnThePitchPipe() {
        becomeEligible()
        launchMain()
        PitchPerfectAnalytics.songAdded()
        repeat(5) { screens.settle() }
        assertEquals(emptyList<Activity>(), asked)
    }

    @Test
    fun noOneIsAskedSoonAfterAPitchSounds() {
        becomeEligible()
        val main = launchMain()
        with(screens) { main.get().show(MainTab.SONGS) }
        PitchPerfectAnalytics.pitchPlayed(PitchPerfectAnalytics.Source.SONG)
        addSong(main, "Shenandoah")
        repeat(3) { screens.settle() }
        assertEquals(emptyList<Activity>(), asked)
    }

    @Test
    fun aNewInstallationIsNeverAsked() {
        val main = launchMain()
        with(screens) { main.get().show(MainTab.SONGS) }
        addSong(main, "Shenandoah")
        repeat(3) { screens.settle() }
        assertEquals(emptyList<Activity>(), asked)
    }

    @Test
    fun creatingASetListIsReported() {
        val main = launchMain()
        with(screens) { main.get().show(MainTab.SONGS) }
        screens.click(TestTags.SET_LIST_ADD_POSITION)
        compose.onNodeWithTag(TestTags.NAME_DIALOG_FIELD).performTextReplacement("Contest Set")
        screens.click(TestTags.NAME_DIALOG_CONFIRM)
        assertEquals(1, named("set_list_created").size)
    }

    @Test
    fun addingSongsToASetListDeletedMeanwhileIsNotReported() {
        val model = SongsModel.get()
        model.defaultSongList.addSong(ComposeScreens.song("Shenandoah"))
        val target = model.createList("Contest Set")
        val addable = AddableSongs(model, target)
        addable.toggleAll()
        // Deleted on another device while Add songs was open.
        model.deleteList(target)
        addable.confirm()
        assertEquals(emptyList<Pair<String, Map<String, String>>>(), named("songs_added_to_set_list"))
    }

    @Test
    fun settingsArriveAsUserPropertiesWhetherChangedHereOrSynced() {
        SettingsModel.classicPitchPipe = true
        assertEquals("classic", properties["pitch_pipe_style"])
        // As the account's snapshot listener applies another device's choices.
        SettingsModel.applyRemoteReferencePitch(432)
        SettingsModel.applyRemoteNoteSound("piano")
        assertEquals("432", properties["reference_pitch"])
        assertEquals("piano", properties["note_sound"])
    }

    @Test
    fun aNoteHeldForMinutesKeepsTheQuietFromWhenItStops() {
        becomeEligible()
        Note.setPlayer(WidgetAwareNotePlayer(ScreenTestSupport.silentPlayer) {})
        val note = Note.getCommonNotes()[0]
        note.play()
        now += 10 * 60 * 1000L
        assertTrue("ten minutes after it started", policy.shouldAsk("1.0"))
        note.stop()
        assertFalse("the quiet starts when the note stops", policy.shouldAsk("1.0"))
        now += ReviewPolicy.QUIET_MILLIS_AFTER_SOUND
        assertTrue(policy.shouldAsk("1.0"))
        Note.setPlayer(ScreenTestSupport.silentPlayer)
    }

    private fun List<String>.distinctConsecutive(): List<String> = filterIndexed { index, name -> index == 0 || this[index - 1] != name }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
    }
}
