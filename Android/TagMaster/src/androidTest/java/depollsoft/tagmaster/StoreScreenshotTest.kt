package depollsoft.tagmaster

import depollsoft.testing.StoreScreenshots
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreScreenshotTest {
    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        Thread.sleep(1000)
        StoreScreenshots.capture(instrumentation, name)
    }

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun awaitContent(description: String, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 120000
        var ready = false
        while (!ready && System.currentTimeMillis() < deadline) {
            main { ready = predicate() }
            Thread.sleep(250)
        }
        check(ready) { "Live content did not load: $description" }
    }

    @Test fun captureStoreScreenshots() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("storeScreenshots") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Capture a returning user's library with optional telemetry declined.
        depollsoft.lib.privacy.PrivacyChoices(context).save(analytics = false, crashes = false)
        // Store scenes show a returning user's library, after viewing the changelog.
        depollsoft.lib.util.Preferences.set("depollsoft.lib.LastVersionSeen",
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode)
        val intent = android.content.Intent(context, TagDetailActivity::class.java)
            .putExtra(TagDetailActivity.TAG_ID_EXTRA, 122)
        ActivityScenario.launch<TagDetailActivity>(intent).use { scenario ->
            lateinit var activity: TagDetailActivity
            scenario.onActivity {
                activity = it
                // Preserve the production request and log failures on subsequent retries.
                it.tagLoader = { id, refresh ->
                    depollsoft.tagmaster.barbershop.Tag.loadTagById(id, refresh).continueWithTask { task ->
                        if (task.isFaulted) android.util.Log.e("StoreScreenshots", "Live tag $id request failed", task.error)
                        task
                    }
                }
            }
            fun onActivity(action: (TagDetailActivity) -> Unit) {
                // Loading animations continuously invalidate the UI. Poll on the
                // main thread without waiting for global Espresso/Looper idleness.
                InstrumentationRegistry.getInstrumentation().runOnMainSync { action(activity) }
            }
            var deadline = System.currentTimeMillis() + 120000
            var loaded = false
            var retries = 0
            var nextRetry = System.currentTimeMillis() + 5000
            while (!loaded && System.currentTimeMillis() < deadline) {
                onActivity {
                    loaded = it.tag != null && !it.isLoading
                    if (!loaded && it.loadFailed && !it.isLoading && retries < 2 &&
                        System.currentTimeMillis() >= nextRetry) {
                        // Retry the real request through the same control a user sees.
                        // Never capture an error state or substitute offline fixtures.
                        println("Retrying live tag 122 after a load failure (retry ${retries + 1})")
                        check(it.findViewById<android.view.View>(R.id.detailRetryButton).performClick())
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
                onActivity {
                    it.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager).setCurrentItem(page, false)
                }
                if (page == 2) {
                    onActivity {
                        it.findViewById<android.view.View>(R.id.leadButton).performClick()
                        it.findViewById<android.view.View>(R.id.playPauseButton).performClick()
                    }
                    val trackDeadline = System.currentTimeMillis() + 120000
                    var ready = false
                    while (!ready && System.currentTimeMillis() < trackDeadline) {
                        onActivity {
                            val player = it.findViewById<MediaPlayerView>(R.id.mediaPlayer)
                            ready = player.audioLength > 0 && !player.isLoading
                        }
                        Thread.sleep(200)
                    }
                    check(ready) { "Live learning track failed to load" }
                    onActivity {
                        val player = it.findViewById<MediaPlayerView>(R.id.mediaPlayer)
                        if (player.isPlaying) it.findViewById<android.view.View>(R.id.playPauseButton).performClick()
                    }
                }
                if (page == 3) {
                    awaitContent("videos") { (activity.findViewById<android.widget.ListView>(R.id.videoList)?.count ?: 0) > 0 }
                    // The first historical video is no longer available; show the next live submissions.
                    onActivity { it.findViewById<android.widget.ListView>(R.id.videoList).setSelection(1) }
                    awaitContent("video thumbnails") {
                        val list = activity.findViewById<android.widget.ListView>(R.id.videoList)
                        (0 until list.childCount).count { list.getChildAt(it).findViewById<android.widget.ImageView>(R.id.videoPreview)?.drawable != null } >= 3
                    }
                }
                capture(name)
            }
        }
        main { listOf(122, 669, 1478).forEach { FavoritesModel.addFavorite(it) } }
        ActivityScenario.launch(MeActivity::class.java).use { scenario ->
            lateinit var activity: MeActivity
            scenario.onActivity { activity = it }
            awaitContent("favorite tag rows") {
                // Home now shows the Lists group above Favorites, so not every favorite fits on a
                // phone's first screen: every favorite row that is on screen must have loaded, and
                // the favorites adapter must hold all three.
                val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.homeList)
                val rows = (0 until list.childCount).map { list.getChildAt(it) }.filterIsInstance<SavedTagItemView>()
                activity.favoritesAdapter.itemCount == 3 && rows.isNotEmpty() &&
                    rows.all { it.tag != null && !it.isLoading && !it.failedToLoad }
            }
            if (activity.hasDetailPane) {
                main { activity.showTag(122) }
                awaitContent("favorite detail pane") { activity.supportFragmentManager.fragments.filterIsInstance<TagDetailFragment>().any { it.tag != null && !it.isLoading } }
            }
            capture("01-home")
        }
        ActivityScenario.launch(TagBrowserActivity::class.java).use { scenario ->
            lateinit var activity: TagBrowserActivity
            scenario.onActivity { activity = it; it.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager).setCurrentItem(3, false) }
            awaitContent("classic tags") {
                val fragment = activity.supportFragmentManager.findFragmentByTag("f3") as? TagQueryFragment
                fragment?.model?.let { !it.isLoading && it.tags.size > 0 } == true
            }
            if (activity.hasDetailPane) {
                main { activity.showTag(2) }
                awaitContent("classic detail pane") { activity.supportFragmentManager.fragments.filterIsInstance<TagDetailFragment>().any { it.tag != null && !it.isLoading } }
            }
            capture("02-browse")
        }
        val query = QueryModel().apply { query = "Lone Prairie"; maxResults = 100 }
        ActivityScenario.launch(TagSearchActivity::class.java).use { scenario ->
            scenario.onActivity { it.findViewById<android.widget.EditText>(R.id.searchTextBox).setText("Lone Prairie") }
            capture("03-search")
        }
        val results = android.content.Intent(context, TagSearchResultsActivity::class.java)
            .putExtra(TagQueryFragment.QUERY_MODEL, depollsoft.lib.json.JsonSerializer.serialize(query).toString())
        ActivityScenario.launch<TagSearchResultsActivity>(results).use { scenario ->
            lateinit var activity: TagSearchResultsActivity
            scenario.onActivity { activity = it }
            awaitContent("search results") {
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as? TagQueryFragment
                fragment?.model?.let { !it.isLoading && it.tags.size > 0 } == true
            }
            if (activity.hasDetailPane) {
                main { activity.showTag(68) }
                awaitContent("search detail pane") { activity.supportFragmentManager.fragments.filterIsInstance<TagDetailFragment>().any { it.tag != null && !it.isLoading } }
            }
            capture("04-results")
        }
    }
}
