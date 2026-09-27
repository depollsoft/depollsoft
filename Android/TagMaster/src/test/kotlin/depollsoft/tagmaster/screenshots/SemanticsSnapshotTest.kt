package depollsoft.tagmaster.screenshots

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.testing.SemanticsSnapshot
import depollsoft.tagmaster.AuthState
import depollsoft.tagmaster.MeActivity
import depollsoft.tagmaster.QueryModel
import depollsoft.tagmaster.ScreenTestSupport
import depollsoft.tagmaster.SettingsActivity
import depollsoft.tagmaster.TagBrowserActivity
import depollsoft.tagmaster.TagDetailActivity
import depollsoft.tagmaster.TagListActivity
import depollsoft.tagmaster.TagSearchActivity
import depollsoft.tagmaster.TagSearchResultsActivity
import depollsoft.tagmaster.TeachableTagsActivity
import depollsoft.tagmaster.screenshots.ScreenshotFixtures.HEART
import depollsoft.tagmaster.screenshots.ScreenshotFixtures.settle
import depollsoft.tagmaster.ui.FooterYear
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.net.URL
import java.util.TimeZone

/**
 * What a screen reader hears and can do on each Tag Master screen, pinned like the pixel goldens
 * (`src/test/semantics`), with the same fixtures. A lost label, role, state, custom action or live
 * region fails here even when the pixels are unchanged.
 */
@RunWith(RobolectricTestRunner::class)
// Semantics are snapshotted on a very tall screen: a lazy list composes only the rows that fit, and
// Linux measures text a little taller than macOS, so on a phone-sized screen the last row (the
// footer) could fall out of the snapshot on CI but not locally.
@Config(application = Application::class, qualifiers = "w411dp-h4000dp-xxhdpi")
class SemanticsSnapshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val driver by lazy { ScreenshotDriver(compose) }
    private var controller: ActivityController<*>? = null
    private val catalog: (URL) -> java.io.InputStream = { ScreenshotFixtures.catalogPage(it) }
    private val defaultZone = TimeZone.getDefault()
    private val app get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        ScreenTestSupport.startClean()
        ScreenTestSupport.clearTagCaches()
        ScreenshotFixtures.clearPrivacyChoices()
        ScreenshotFixtures.cacheTags()
        ScreenshotFixtures.pinVersion()
        FooterYear.pinned = 2026
        AuthState.setTestSource { false }
    }

    @After
    fun tearDown() {
        AuthState.setTestSource(null)
        FooterYear.pinned = null
        TimeZone.setDefault(defaultZone)
        ScreenTestSupport.finish(controller)
        ScreenTestSupport.clearTagCaches()
    }

    private fun <A : Activity> launch(
        clazz: Class<A>,
        intent: Intent? = null,
    ): A {
        val created = ScreenTestSupport.build(clazz, intent)
        controller = created
        created.setup()
        settle()
        ScreenshotFixtures.dismissStartupDialogs()
        settle()
        return created.get()
    }

    private fun verify(name: String) {
        repeat(3) {
            settle()
            compose.waitForIdle()
        }
        SemanticsSnapshot.verify(compose, name)
    }

    private fun screen(
        name: String,
        block: () -> Unit,
    ) = ScreenTestSupport.withTransport({ catalog(it) }) {
        block()
        verify(name)
    }

    private fun detail(page: Int) =
        launch(TagDetailActivity::class.java, Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, HEART))
            .also { driver.selectDetailPage(it, page) }

    private fun results(query: String) =
        Intent(app, TagSearchResultsActivity::class.java)
            .putExtra(
                TagSearchResultsActivity.QUERY_MODEL,
                JsonSerializer
                    .serialize(
                        QueryModel().apply {
                            this.query = query
                            resultSetSize = 20
                            maxResults = 50
                        },
                    ).toString(),
            )

    @Test
    fun homeEmpty() = screen("home_empty") { launch(MeActivity::class.java) }

    @Test
    fun homePopulated() =
        screen("home_populated") {
            ScreenshotFixtures.populateLists()
            launch(MeActivity::class.java)
        }

    @Test
    fun homeEditing() =
        screen("home_editing") {
            ScreenshotFixtures.populateLists()
            driver.startEditing(launch(MeActivity::class.java))
        }

    @Test
    fun newListDialog() =
        screen("dialog_new_list") {
            launch(MeActivity::class.java)
            driver.showNewListDialog()
        }

    @Test
    fun openTagDialog() =
        screen("dialog_open_tag") {
            launch(MeActivity::class.java)
            driver.showOpenTagDialog()
        }

    @Test
    fun teachable() =
        screen("teachable_populated") {
            ScreenshotFixtures.populateLists()
            launch(TeachableTagsActivity::class.java)
        }

    @Test
    fun customListEditing() =
        screen("list_editing") {
            val (afterglow, _) = ScreenshotFixtures.populateLists()
            driver.startEditing(launch(TagListActivity::class.java, TagListActivity.intent(app, afterglow)))
        }

    @Test
    fun browse() = screen("browse") { launch(TagBrowserActivity::class.java) }

    @Test
    fun search() = screen("search") { launch(TagSearchActivity::class.java) }

    @Test
    fun results() = screen("results") { launch(TagSearchResultsActivity::class.java, results("heart")) }

    @Test
    fun detailSummary() =
        screen("detail_summary") {
            ScreenshotFixtures.populateLists()
            detail(0)
        }

    @Test
    fun detailDetails() = screen("detail_details") { detail(1) }

    @Test
    fun detailTracks() = screen("detail_tracks") { detail(2) }

    @Test
    fun detailVideos() = screen("detail_videos") { detail(3) }

    @Test
    fun listPicker() =
        screen("dialog_list_picker") {
            ScreenshotFixtures.populateLists()
            detail(0)
            driver.showListPicker()
        }

    @Test
    fun ratingDialog() =
        screen("dialog_rating") {
            detail(0)
            driver.showRatingDialog()
        }

    @Test
    fun settings() = screen("settings") { launch(SettingsActivity::class.java) }

    @Test
    @Config(qualifiers = "w1280dp-h4000dp-xhdpi")
    fun tabletHomeWithTag() =
        screen("tablet_home_tag") {
            ScreenshotFixtures.populateLists()
            driver.showTag(launch(MeActivity::class.java), HEART)
        }
}
