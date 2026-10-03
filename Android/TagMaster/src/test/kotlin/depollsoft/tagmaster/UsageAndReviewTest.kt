package depollsoft.tagmaster

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.performTextInput
import depollsoft.lib.analytics.UsageAnalytics
import depollsoft.lib.review.ReviewPolicy
import depollsoft.lib.review.ReviewPrompt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * What Tag Master reports to analytics as someone opens and keeps tags (docs/analytics.md), and the
 * one place the review card may follow: Home, a few quiet seconds after a list task.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class UsageAndReviewTest : ComposeScreenTest() {
    private val fixture = ScreenTestSupport.fixtureTag()
    private val events = mutableListOf<Pair<String, Map<String, String>>>()
    private val asked = mutableListOf<Activity>()
    private val prefs get() = app.getSharedPreferences("usage_review_test", Context.MODE_PRIVATE)
    private var now = 1_790_881_200_000L
    private lateinit var policy: ReviewPolicy

    @Before
    fun record() {
        prefs.edit().clear().commit()
        policy = ReviewPolicy(prefs, { now })
        UsageAnalytics.sink =
            object : UsageAnalytics.Sink {
                override fun logEvent(name: String, params: Map<String, String>) {
                    events += name to params
                }

                override fun setUserProperty(name: String, value: String) {}
            }
        ReviewPrompt.resetForTesting()
        ReviewPrompt.policyForTesting = policy
        ReviewPrompt.showStoreReview = { asked += it }
    }

    @After
    fun reset() {
        UsageAnalytics.resetForTesting()
        ReviewPrompt.resetForTesting()
    }

    private fun named(name: String) = events.filter { it.first == name }

    private fun screenViews() = named(UsageAnalytics.SCREEN_VIEW).map { it.second.getValue(UsageAnalytics.SCREEN_NAME) }

    /** Five days of use over two weeks, and nothing played for a while. */
    private fun becomeEligible() {
        repeat(ReviewPolicy.MIN_ACTIVE_DAYS) {
            policy.recordUse()
            now += DAY
        }
        now += ReviewPolicy.MIN_DAYS_SINCE_FIRST_USE * DAY
    }

    private fun letTimePass(millis: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))
        idle()
    }

    private fun detail(): TagDetailActivity {
        ScreenTestSupport.cacheOnDisk(fixture)
        val activity =
            launch(
                TagDetailActivity::class.java,
                Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, fixture.id),
            )
        ScreenTestSupport.awaitTagLoaded(activity)
        controller!!.windowFocusChanged(true)
        idle()
        return activity
    }

    private fun home(): MeActivity =
        launch(MeActivity::class.java).also {
            controller!!.windowFocusChanged(true)
            idle()
        }

    private fun createList(name: String) {
        click("newListButton")
        node("listNameInput").performTextInput(name)
        click("listNameConfirm")
    }

    @Test
    fun openingATagReportsItAndThePagesShown() {
        detail()
        assertEquals(1, named("tag_viewed").size)
        click("detailTab:2")
        click("detailTab:3")
        assertEquals(listOf("tag_summary", "tag_tracks", "tag_videos"), screenViews().filter { it.startsWith("tag_") }.distinct())
    }

    @Test
    fun eachListScreenReportsItsName() {
        home()
        assertTrue(screenViews().contains("home"))
    }

    @Test
    fun favoritingFromTheToolbarIsReported() {
        detail()
        click("addFavorite")
        assertEquals(listOf(mapOf("list" to "favorites")), named("tag_added_to_list").map { it.second })
    }

    @Test
    fun addingThroughThePickerIsReportedAndRemovingIsNot() {
        val key = TagLists.create("Afterglow set")
        detail()
        click("chip:add")
        click("pickerRow:$key")
        click("pickerRow:$key")
        click("listPickerDone")
        assertEquals(listOf(mapOf("list" to "custom")), named("tag_added_to_list").map { it.second })
    }

    @Test
    fun theKeyNoteIsAPitchPlayedFromTheTag() {
        detail()
        click("playKeyNoteButton")
        assertEquals(listOf(mapOf("source" to "tag")), named("pitch_played").map { it.second })
    }

    @Test
    fun homeAsksAfterANewListAndThreeQuietSeconds() {
        becomeEligible()
        val activity = home()
        createList("Afterglow set")
        assertEquals(1, named("tag_list_created").size)
        // ReviewPromptTest pins the three seconds; Compose's test clock runs on while dialogs close.
        letTimePass(ReviewPrompt.CALM_MILLIS)
        assertEquals(listOf<Activity>(activity), asked)
        assertEquals(1, named(ReviewPrompt.ASKED_EVENT).size)
    }

    @Test
    fun noOneIsAskedOnATag() {
        becomeEligible()
        detail()
        click("addFavorite")
        letTimePass(10_000)
        assertEquals(emptyList<Activity>(), asked)
    }

    @Test
    fun noOneIsAskedRightAfterTheKeyNote() {
        becomeEligible()
        detail()
        click("playKeyNoteButton")
        controller!!.pause().stop().destroy()
        home()
        createList("Afterglow set")
        letTimePass(10_000)
        assertEquals(emptyList<Activity>(), asked)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
    fun comingBackToATagBesideItsListReportsOnlyTheTag() {
        ScreenTestSupport.cacheOnDisk(fixture)
        TeachableTagsModel.addTeachableTag(fixture.id)
        val activity = launch(TeachableTagsActivity::class.java)
        activity.showTag(fixture.id)
        val detail = activity.tagPane.detail!!
        ScreenTestSupport.await("the pane's tag to load") { !detail.isLoading && detail.tag != null }
        idle()
        events.clear()
        controller!!.pause().stop()
        controller!!.restart().start().resume()
        idle()
        assertEquals(listOf("tag_summary"), screenViews())
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
    }
}
