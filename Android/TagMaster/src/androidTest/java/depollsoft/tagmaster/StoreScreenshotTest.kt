package depollsoft.tagmaster

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import depollsoft.testing.StoreScreenshots
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Captures the Play Store screenshots from live catalog content (`scripts/release/capture.py`).
 * Opt in with the `storeScreenshots=true` instrumentation argument.
 */
@RunWith(AndroidJUnit4::class)
class StoreScreenshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun capture(name: String) {
        instrumentation.waitForIdleSync()
        Thread.sleep(1000)
        StoreScreenshots.capture(instrumentation, name)
    }

    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    /** Polls [predicate] on the main thread; loaders animate, so global idleness never comes. */
    private fun awaitContent(
        description: String,
        predicate: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + 120000
        var ready = false
        while (!ready && System.currentTimeMillis() < deadline) {
            main { ready = predicate() }
            if (!ready) Thread.sleep(250)
        }
        check(ready) { "Live content did not load: $description" }
    }

    private fun tagged(prefix: String) =
        SemanticsMatcher("test tag starts with $prefix") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
        }

    private fun count(prefix: String) = compose.onAllNodes(tagged(prefix), useUnmergedTree = true).fetchSemanticsNodes().size

    private fun paneLoaded(pane: TagPaneState) = pane.detail?.let { it.tag != null && !it.isLoading } == true

    @Test
    fun captureStoreScreenshots() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("storeScreenshots") == "true")
        val context = instrumentation.targetContext
        // Capture a returning user's library with optional telemetry declined.
        depollsoft.lib.privacy.PrivacyChoices(context).save(analytics = false, crashes = false)
        // Store scenes show a returning user's library, after viewing the changelog.
        depollsoft.lib.util.Preferences.set(
            "depollsoft.lib.LastVersionSeen",
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode,
        )
        val intent =
            android.content.Intent(context, TagDetailActivity::class.java)
                .putExtra(TagDetailActivity.TAG_ID_EXTRA, 122)
        ActivityScenario.launch<TagDetailActivity>(intent).use { scenario ->
            lateinit var activity: TagDetailActivity
            scenario.onActivity { activity = it }
            var deadline = System.currentTimeMillis() + 120000
            var loaded = false
            var retries = 0
            var nextRetry = System.currentTimeMillis() + 5000
            while (!loaded && System.currentTimeMillis() < deadline) {
                main {
                    loaded = activity.tag != null && !activity.isLoading
                    if (!loaded && activity.loadFailed && !activity.isLoading && retries < 2 && System.currentTimeMillis() >= nextRetry) {
                        // Retry the real request, as the Retry button does. Never capture an error
                        // state or substitute offline fixtures.
                        println("Retrying live tag 122 after a load failure (retry ${retries + 1})")
                        activity.detail.retry()
                        retries++
                        nextRetry = System.currentTimeMillis() + 5000
                        // A slow failed connection must not consume the next attempt's budget.
                        deadline = System.currentTimeMillis() + 120000
                    }
                }
                Thread.sleep(200)
            }
            check(loaded) { "Live tag 122 did not load; refusing empty store screenshots" }
            for ((page, name) in listOf(0 to "05-summary", 1 to "06-details", 2 to "07-tracks", 3 to "08-videos")) {
                main { activity.detail.page = page }
                compose.waitForIdle()
                if (page == 2) {
                    compose.onNodeWithTag("part:2").performSemanticsAction(SemanticsActions.OnClick)
                    compose.onNodeWithTag("playPause").performSemanticsAction(SemanticsActions.OnClick)
                    // The counter reads "position/lengths" once the track has been prepared.
                    awaitContent("the learning track") {
                        val counter =
                            compose
                                .onAllNodes(SemanticsMatcher("track counter") { node ->
                                    node.config.getOrNull(SemanticsProperties.Text)?.any { it.text.matches(Regex("[0-9.]+/[0-9.]+s")) && !it.text.endsWith("/0.0s") } == true
                                }, useUnmergedTree = true)
                                .fetchSemanticsNodes()
                        counter.isNotEmpty()
                    }
                    val playing =
                        compose.onNodeWithTag("playPause").fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.contains(
                            context.getString(R.string.Pause),
                        ) == true
                    if (playing) compose.onNodeWithTag("playPause").performSemanticsAction(SemanticsActions.OnClick)
                }
                if (page == 3) {
                    awaitContent("videos") { count("video:") > 1 }
                    // The first historical video is no longer available; show the next live submissions.
                    val second = compose.onAllNodes(tagged("video:"), useUnmergedTree = true).fetchSemanticsNodes()[1]
                    compose.onNodeWithTag(second.config[SemanticsProperties.TestTag], useUnmergedTree = true).performScrollTo()
                    Thread.sleep(3000) // Thumbnails arrive from YouTube.
                }
                capture(name)
            }
        }
        main { listOf(122, 669, 1478).forEach { FavoritesModel.addFavorite(it) } }
        ActivityScenario.launch(MeActivity::class.java).use { scenario ->
            lateinit var activity: MeActivity
            scenario.onActivity { activity = it }
            awaitContent("favorite tag rows") {
                // A loaded row shows its metadata; not every favorite fits on a phone's first screen.
                val rows = compose.onAllNodes(tagged("savedTag:"), useUnmergedTree = true).fetchSemanticsNodes()
                rows.isNotEmpty() &&
                    rows.all { row ->
                        row.children.flatMap { it.children + it }.any { child ->
                            child.config.getOrNull(SemanticsProperties.Text)?.any { it.text.startsWith("ID:") } == true
                        }
                    }
            }
            if (activity.hasDetailPane) {
                main { activity.showTag(122) }
                awaitContent("favorite detail pane") { paneLoaded(activity.tagPane) }
            }
            capture("01-home")
        }
        ActivityScenario.launch(TagBrowserActivity::class.java).use { scenario ->
            lateinit var activity: TagBrowserActivity
            scenario.onActivity { activity = it }
            compose.onNodeWithTag("browseTab:3").performSemanticsAction(SemanticsActions.OnClick)
            awaitContent("classic tags") { activity.models[3].let { !it.isLoading && it.tags.isNotEmpty() } }
            if (activity.hasDetailPane) {
                main { activity.showTag(2) }
                awaitContent("classic detail pane") { paneLoaded(activity.tagPane) }
            }
            capture("02-browse")
        }
        val query =
            QueryModel().apply {
                this.query = "Lone Prairie"
                maxResults = 100
            }
        ActivityScenario.launch(TagSearchActivity::class.java).use {
            compose.onNodeWithTag("searchTextBox").performTextInput("Lone Prairie")
            capture("03-search")
        }
        val results =
            android.content.Intent(context, TagSearchResultsActivity::class.java)
                .putExtra(TagSearchResultsActivity.QUERY_MODEL, depollsoft.lib.json.JsonSerializer.serialize(query).toString())
        ActivityScenario.launch<TagSearchResultsActivity>(results).use { scenario ->
            lateinit var activity: TagSearchResultsActivity
            scenario.onActivity { activity = it }
            awaitContent("search results") { activity.model.let { !it.isLoading && it.tags.isNotEmpty() } }
            if (activity.hasDetailPane) {
                main { activity.showTag(68) }
                awaitContent("search detail pane") { paneLoaded(activity.tagPane) }
            }
            capture("04-results")
        }
    }
}
