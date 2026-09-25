package depollsoft.tagmaster.screenshots

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import bolts.Task
import bolts.TaskCompletionSource
import depollsoft.lib.json.JsonSerializer
import depollsoft.tagmaster.AuthState
import depollsoft.tagmaster.MeActivity
import depollsoft.tagmaster.QueryModel
import depollsoft.tagmaster.ScreenTestSupport
import depollsoft.tagmaster.SettingsActivity
import depollsoft.tagmaster.SheetMusicActivity
import depollsoft.tagmaster.TagBrowserActivity
import depollsoft.tagmaster.TagDetailActivity
import depollsoft.tagmaster.TagListActivity
import depollsoft.tagmaster.TagSearchActivity
import depollsoft.tagmaster.TagSearchResultsActivity
import depollsoft.tagmaster.TeachableTagsActivity
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.screenshots.ScreenshotFixtures.HEART
import depollsoft.tagmaster.screenshots.ScreenshotFixtures.settle
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.IOException
import java.net.URL

/**
 * Pixel goldens for every Tag Master screen and the states that change how it looks.
 *
 * The goldens in `src/test/screenshots` were first recorded from the View implementation and the
 * Compose port was diffed against them (`./gradlew :TagMaster:compareRoborazziDebug`). State is
 * set up through the models, intents, disk cache and network transport; the few steps that depend
 * on the UI toolkit live in [ScreenshotDriver].
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = PHONE)
class TagMasterScreenshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val driver by lazy { ScreenshotDriver(compose) }
    private var controller: ActivityController<*>? = null
    private var catalog: (URL) -> java.io.InputStream = { ScreenshotFixtures.catalogPage(it) }

    @Before
    fun setUp() {
        ScreenTestSupport.startClean()
        ScreenTestSupport.clearTagCaches()
        ScreenshotFixtures.forgetPrivacy()
        ScreenshotFixtures.cacheTags()
        ScreenshotFixtures.pinVersion()
        AuthState.setTestSource { false }
    }

    @After
    fun tearDown() {
        AuthState.setTestSource(null)
        ScreenTestSupport.finish(controller)
        ScreenTestSupport.clearTagCaches()
    }

    private fun <A : Activity> launch(
        clazz: Class<A>,
        intent: Intent? = null,
        before: (A) -> Unit = {},
    ): A {
        val created = ScreenTestSupport.build(clazz, intent)
        before(created.get())
        controller = created
        created.setup()
        settle()
        ScreenshotFixtures.dismissStartupDialogs()
        settle()
        return created.get()
    }

    /** Lets background loads report back and Compose recompose until both are quiet. */
    private fun settleAll() {
        repeat(3) {
            settle()
            compose.waitForIdle()
        }
    }

    private fun capture(
        activity: Activity,
        name: String,
    ) {
        settleAll()
        activity.window.decorView.captureRoboImage("src/test/screenshots/$name.png")
    }

    private fun captureScreen(name: String) {
        settleAll()
        captureScreenRoboImage("src/test/screenshots/$name.png")
    }

    private fun <T> withCatalog(block: () -> T): T = ScreenTestSupport.withTransport({ catalog(it) }, block)

    private fun detailIntent(id: Int = HEART) =
        Intent(org.robolectric.RuntimeEnvironment.getApplication(), TagDetailActivity::class.java)
            .putExtra(TagDetailActivity.TAG_ID_EXTRA, id)

    private fun resultsIntent(query: String) =
        Intent(org.robolectric.RuntimeEnvironment.getApplication(), TagSearchResultsActivity::class.java)
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

    // ==================== Home ====================

    @Test
    fun homeEmpty() =
        withCatalog {
            capture(launch(MeActivity::class.java), "home_empty")
        }

    @Test
    fun homePopulated() =
        withCatalog {
            ScreenshotFixtures.populateLists()
            capture(launch(MeActivity::class.java), "home_populated")
        }

    @Test
    fun homeEditing() =
        withCatalog {
            ScreenshotFixtures.populateLists()
            val activity = launch(MeActivity::class.java)
            driver.startEditing(activity)
            capture(activity, "home_editing")
        }

    @Test
    @Config(qualifiers = PHONE_NIGHT)
    fun homePopulatedNight() =
        withCatalog {
            ScreenshotFixtures.populateLists()
            capture(launch(MeActivity::class.java), "home_populated_night")
        }

    @Test
    fun homeNewListDialog() =
        withCatalog {
            val activity = launch(MeActivity::class.java)
            driver.showNewListDialog()
            captureScreen("dialog_new_list")
        }

    @Test
    fun homeRenameListDialog() =
        withCatalog {
            val (afterglow, _) = ScreenshotFixtures.populateLists()
            val activity = launch(MeActivity::class.java)
            driver.showRenameListDialog(afterglow)
            captureScreen("dialog_rename_list")
        }

    @Test
    fun homeDeleteListDialog() =
        withCatalog {
            val (afterglow, _) = ScreenshotFixtures.populateLists()
            val activity = launch(MeActivity::class.java)
            driver.showDeleteListDialog(afterglow)
            captureScreen("dialog_delete_list")
        }

    @Test
    fun homeOpenTagDialog() =
        withCatalog {
            val activity = launch(MeActivity::class.java)
            driver.showOpenTagDialog()
            captureScreen("dialog_open_tag")
        }

    // ==================== Lists ====================

    @Test
    fun teachableEmpty() =
        withCatalog {
            capture(launch(TeachableTagsActivity::class.java), "teachable_empty")
        }

    @Test
    fun teachablePopulated() =
        withCatalog {
            ScreenshotFixtures.populateLists()
            capture(launch(TeachableTagsActivity::class.java), "teachable_populated")
        }

    @Test
    fun customListPopulated() =
        withCatalog {
            val (afterglow, _) = ScreenshotFixtures.populateLists()
            val app = org.robolectric.RuntimeEnvironment.getApplication()
            capture(launch(TagListActivity::class.java, TagListActivity.intent(app, afterglow)), "list_populated")
        }

    @Test
    fun customListEditing() =
        withCatalog {
            val (afterglow, _) = ScreenshotFixtures.populateLists()
            val app = org.robolectric.RuntimeEnvironment.getApplication()
            val activity = launch(TagListActivity::class.java, TagListActivity.intent(app, afterglow))
            driver.startEditing(activity)
            capture(activity, "list_editing")
        }

    @Test
    fun customListEmpty() =
        withCatalog {
            val (_, easy) = ScreenshotFixtures.populateLists()
            val app = org.robolectric.RuntimeEnvironment.getApplication()
            capture(launch(TagListActivity::class.java, TagListActivity.intent(app, easy)), "list_empty")
        }

    // ==================== Browse, search and results ====================

    @Test
    fun browse() =
        withCatalog {
            capture(launch(TagBrowserActivity::class.java), "browse")
        }

    @Test
    @Config(qualifiers = PHONE_NIGHT)
    fun browseNight() =
        withCatalog {
            capture(launch(TagBrowserActivity::class.java), "browse_night")
        }

    @Test
    fun search() =
        withCatalog {
            capture(launch(TagSearchActivity::class.java), "search")
        }

    @Test
    fun results() =
        withCatalog {
            capture(launch(TagSearchResultsActivity::class.java, resultsIntent("heart")), "results")
        }

    @Test
    fun resultsEmpty() {
        catalog = { ScreenshotFixtures.catalogPage(it, available = 0) }
        withCatalog {
            capture(launch(TagSearchResultsActivity::class.java, resultsIntent("zzz")), "results_empty")
        }
    }

    @Test
    fun resultsFailed() {
        catalog = { throw IOException("The connection was interrupted.") }
        withCatalog {
            capture(launch(TagSearchResultsActivity::class.java, resultsIntent("heart")), "results_failed")
        }
    }

    // ==================== Tag detail ====================

    private fun detail(
        page: Int,
        name: String,
        id: Int = HEART,
    ) = withCatalog {
        val activity = launch(TagDetailActivity::class.java, detailIntent(id))
        driver.selectDetailPage(activity, page)
        capture(activity, name)
    }

    @Test
    fun detailSummary() {
        ScreenshotFixtures.populateLists()
        detail(0, "detail_summary")
    }

    @Test
    fun detailSummarySparse() = detail(0, "detail_summary_sparse", ScreenshotFixtures.SUNSHINE)

    @Test
    fun detailDetails() = detail(1, "detail_details")

    @Test
    fun detailTracks() = detail(2, "detail_tracks")

    @Test
    fun detailTracksEmpty() = detail(2, "detail_tracks_empty", ScreenshotFixtures.SUNSHINE)

    @Test
    fun detailVideos() = detail(3, "detail_videos")

    @Test
    fun detailVideosEmpty() = detail(3, "detail_videos_empty", ScreenshotFixtures.SUNSHINE)

    @Test
    @Config(qualifiers = PHONE_NIGHT)
    fun detailSummaryNight() {
        ScreenshotFixtures.populateLists()
        detail(0, "detail_summary_night")
    }

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun detailSummaryLandscape() = detail(0, "detail_summary_land")

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun detailTracksLandscape() = detail(2, "detail_tracks_land")

    @Test
    fun detailLoading() =
        withCatalog {
            val pending = TaskCompletionSource<Tag>()
            val activity =
                launch(TagDetailActivity::class.java, detailIntent()) {
                    it.tagLoader = { _, _ -> pending.task }
                }
            capture(activity, "detail_loading")
        }

    @Test
    fun detailFailed() =
        withCatalog {
            val activity =
                launch(TagDetailActivity::class.java, detailIntent()) {
                    it.tagLoader = { _, _ -> Task.forError(IOException("offline")) }
                }
            capture(activity, "detail_failed")
        }

    @Test
    fun detailListPicker() =
        withCatalog {
            ScreenshotFixtures.populateLists()
            val activity = launch(TagDetailActivity::class.java, detailIntent())
            driver.showListPicker()
            captureScreen("dialog_list_picker")
        }

    @Test
    fun detailRatingDialog() =
        withCatalog {
            val activity = launch(TagDetailActivity::class.java, detailIntent())
            driver.showRatingDialog()
            captureScreen("dialog_rating")
        }

    // ==================== Sheet music and settings ====================

    @Test
    fun sheetMusic() =
        withCatalog {
            val app = org.robolectric.RuntimeEnvironment.getApplication()
            val intent =
                Intent(Intent.ACTION_VIEW)
                    .setClass(app, SheetMusicActivity::class.java)
                    .setDataAndType(Uri.fromFile(ScreenshotFixtures.sheetImage()), "image/png")
                    .putExtra("tagId", HEART)
            capture(launch(SheetMusicActivity::class.java, intent), "sheet_music")
        }

    @Test
    fun settingsSignedOut() =
        withCatalog {
            capture(launch(SettingsActivity::class.java), "settings")
        }

    @Test
    @Config(qualifiers = PHONE_NIGHT)
    fun settingsNight() =
        withCatalog {
            capture(launch(SettingsActivity::class.java), "settings_night")
        }

    // ==================== A fractional density ====================

    // A 420dpi phone, whose density is not a whole number: text and View-style dimensions round
    // differently there than at xxhdpi, so these keep that rounding matched.
    @Test
    @Config(qualifiers = PHONE_420)
    fun homePopulated420() =
        withCatalog {
            ScreenshotFixtures.populateLists()
            capture(launch(MeActivity::class.java), "dpi420_home_populated")
        }

    @Test
    @Config(qualifiers = PHONE_420)
    fun results420() =
        withCatalog {
            capture(launch(TagSearchResultsActivity::class.java, resultsIntent("heart")), "dpi420_results")
        }

    @Test
    @Config(qualifiers = PHONE_420)
    fun detailSummary420() {
        ScreenshotFixtures.populateLists()
        detail(0, "dpi420_detail_summary")
    }

    @Test
    @Config(qualifiers = PHONE_420)
    fun settings420() =
        withCatalog {
            capture(launch(SettingsActivity::class.java), "dpi420_settings")
        }

    // ==================== Tablet list and detail ====================

    @Test
    @Config(qualifiers = TABLET)
    fun tabletHomeNothingChosen() =
        withCatalog {
            ScreenshotFixtures.populateLists()
            capture(launch(MeActivity::class.java), "tablet_home")
        }

    @Test
    @Config(qualifiers = TABLET)
    fun tabletHomeWithTag() =
        withCatalog {
            ScreenshotFixtures.populateLists()
            val activity = launch(MeActivity::class.java)
            driver.showTag(activity, HEART)
            capture(activity, "tablet_home_tag")
        }

    @Test
    @Config(qualifiers = TABLET)
    fun tabletBrowseWithTag() =
        withCatalog {
            val activity = launch(TagBrowserActivity::class.java)
            driver.showTag(activity, 2147483200)
            capture(activity, "tablet_browse_tag")
        }

    @Test
    @Config(qualifiers = TABLET)
    fun tabletListWithTagDetails() =
        withCatalog {
            val (afterglow, _) = ScreenshotFixtures.populateLists()
            val app = org.robolectric.RuntimeEnvironment.getApplication()
            val activity = launch(TagListActivity::class.java, TagListActivity.intent(app, afterglow))
            driver.showTag(activity, HEART)
            settle()
            driver.selectDetailPage(activity, 1)
            capture(activity, "tablet_list_tag_details")
        }

    @Test
    @Config(qualifiers = TABLET_NIGHT)
    fun tabletTeachableNight() =
        withCatalog {
            ScreenshotFixtures.populateLists()
            val activity = launch(TeachableTagsActivity::class.java)
            driver.showTag(activity, HEART)
            capture(activity, "tablet_teachable_tag_night")
        }
}

private const val PHONE = "w411dp-h891dp-xxhdpi"
private const val PHONE_420 = "w411dp-h891dp-420dpi"
private const val LANDSCAPE = "w891dp-h411dp-land-xxhdpi"
private const val TABLET = "w1280dp-h800dp-land-xhdpi"
private const val PHONE_NIGHT = "w411dp-h891dp-night-xxhdpi"
private const val TABLET_NIGHT = "w1280dp-h800dp-land-night-xhdpi"
