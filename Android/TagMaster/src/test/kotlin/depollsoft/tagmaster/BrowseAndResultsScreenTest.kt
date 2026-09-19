package depollsoft.tagmaster

import android.app.Application
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.viewpager2.widget.ViewPager2
import depollsoft.tagmaster.ScreenTestSupport.assertDisplayed
import depollsoft.tagmaster.ScreenTestSupport.idle
import depollsoft.tagmaster.barbershop.Tag
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Browse's four sort modes and the results screen's empty, error and pagination states, migrated
 * from the instrumented `LayoutRegressionTest`.
 *
 * Both screens are launched with their query model pre-supplied through the platform's
 * `ActivityLifecycleCallbacks`, so no catalog request is made — the seam the instrumented original
 * used. The equal-tab-width geometry these cases also checked is already covered on the JVM by
 * `BottomTabsLayoutTest`, so it is not repeated.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BrowseAndResultsScreenTest {
    private var controller: ActivityController<*>? = null

    @Before
    fun setUp() {
        ScreenTestSupport.startClean()
        ScreenTestSupport.clearTagCaches()
    }

    @After
    fun tearDown() {
        // Let go of any request thread still waiting on an answer before the activity closes.
        answers.values.forEach { queue -> repeat(4) { queue.offer(true) } }
        ScreenTestSupport.finish(controller)
        answers.clear()
        requested.clear()
        ScreenTestSupport.clearTagCaches()
    }

    // ==================== A controlled catalog on the real transport ====================

    /**
     * Every catalog request this class makes is a real one: production builds the URL, opens the
     * stream and parses the XML. Only the bytes are the test's, and only when it offers them.
     *
     * [answers] holds one scripted outcome per *attempt* at a page, keyed by the page's 0-based
     * start. A request for a scripted page blocks on the request thread until the test offers an
     * outcome, so a page stays genuinely pending for as long as the test wants and the request log
     * shows exactly which pages production asked for. Unscripted pages answer immediately.
     */
    private val requested = CopyOnWriteArrayList<URL>()
    private val answers = ConcurrentHashMap<Int, LinkedBlockingQueue<Boolean>>()
    private val pageSize = 20
    private var available = 26

    /** Take control of every attempt at the page starting at [start]. */
    private fun script(start: Int): LinkedBlockingQueue<Boolean> = answers.computeIfAbsent(start) { LinkedBlockingQueue() }

    private fun startOf(url: URL): Int =
        Regex("[?&]start=(\\d+)")
            .find(url.toString())!!
            .groupValues[1]
            .toInt() - 1

    private fun respond(url: URL): InputStream {
        requested.add(url)
        val start = startOf(url)
        val scripted = answers[start]
        val ok =
            if (scripted == null) {
                true
            } else {
                scripted.poll(30, TimeUnit.SECONDS)
                    ?: throw IOException("No scripted answer was offered for the page at $start")
            }
        if (!ok) {
            throw IOException(
                "The connection was interrupted while loading the next group of tags. Please try again.",
            )
        }
        val count = maxOf(0, minOf(pageSize, available - start))
        val body =
            buildString {
                append("<tags count=\"$count\" available=\"$available\">")
                for (index in start until start + count) {
                    append("<tag>")
                    append("<id>${2147483020 + index}</id>")
                    append("<Title>${title(index)}</Title>")
                    append("<AltTitle>An alternate title that also wraps</AltTitle>")
                    append("<Parts>4</Parts><WritKey>C</WritKey><Rating>4.00</Rating>")
                    append("<Downloaded>${1000 + index}</Downloaded>")
                    append("</tag>")
                }
                append("</tags>")
            }
        return ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
    }

    private fun title(index: Int) = "A long arrangement title for the singers gathered around the room $index"

    private fun launchResults(
        query: String,
        parts: Int? = null,
    ): TagSearchResultsActivity {
        val created = ScreenTestSupport.build(TagSearchResultsActivity::class.java)
        ScreenTestSupport.prepareQueryModels(created.get()) { model ->
            model.query = query
            model.parts = parts
            model.resultSetSize = pageSize
            model.maxResults = 50
        }
        created.setup().visible()
        idle()
        controller = created
        return created.get()
    }

    /** The fragment's refresh action, which is the retry this screen offers. */
    private fun retryThrough(
        activity: androidx.appcompat.app.AppCompatActivity,
        fragment: TagQueryFragment,
    ) {
        val menu = MenuBuilder(activity)
        fragment.onCreateOptionsMenu(menu, activity.menuInflater)
        val retry = menu.findItem(R.id.refreshMenuItem)
        assertNotNull("the screen offers a refresh action to retry with", retry)
        assertTrue("and it is available", retry.isEnabled && retry.isVisible)
        menu.performIdentifierAction(R.id.refreshMenuItem, 0)
        idle()
    }

    private fun TagSearchResultsActivity.list(): ListView = findViewById(R.id.queryResultListView)

    private fun TagSearchResultsActivity.pole(): BarberPoleLoadingView = findViewById(R.id.loadingProgressBar)

    private fun tag(index: Int) =
        Tag().apply {
            id = 2147483020 + index
            title = "A long arrangement title for the singers gathered around the room $index"
            alternativeTitle = "An alternate title that also wraps"
            parts = 4
            writtenKey = "C"
        }

    private fun <A : android.app.Activity> launchWithLocalData(clazz: Class<A>): ActivityController<A> =
        ScreenTestSupport.withLocalData({ _, _ -> bolts.Task.forResult(ScreenTestSupport.fixtureTag()) }) {
            ScreenTestSupport.build(clazz).also {
                it.setup()
                idle()
            }
        }

    private fun resultsFragment(activity: androidx.appcompat.app.AppCompatActivity) =
        activity.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment

    // ==================== Browse ====================

    @Test
    fun everyBrowseModeSortsItsOwnPageAndRendersItsRows() {
        val created = launchWithLocalData(TagBrowserActivity::class.java)
        controller = created
        val activity = created.get()
        val sorts = listOf("Posted", "Rating", "Downloaded", "Classic")

        for (position in 0..3) {
            activity.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(position, false)
            idle()
            val fragment = activity.supportFragmentManager.findFragmentByTag("f$position") as TagQueryFragment
            assertEquals(
                "page $position browses by ${sorts[position]}",
                sorts[position],
                fragment.model!!.sortBy.toString(),
            )

            ScreenTestSupport.awaitQuiet(fragment.model!!)
            fragment.model!!.tags.addAll((0..11).map { tag(it) })
            idle()

            val list = fragment.requireView().findViewById<ListView>(R.id.queryResultListView)
            assertTrue("page $position lists its results", list.height > 0)
            assertEquals(12, list.adapter.count)
            list.setSelection(0)
            idle()
            val title = list.getChildAt(0).findViewById<TextView>(R.id.titleTextView)
            assertTrue("a long title is laid out", title.layout.lineCount > 0)
            for (line in 0 until title.layout.lineCount) {
                assertEquals("a browse row never ellipsizes its title", 0, title.layout.getEllipsisCount(line))
            }
            assertTrue(
                "and every line fits",
                title.layout.height <= title.height - title.compoundPaddingTop - title.compoundPaddingBottom,
            )
        }
    }

    // ==================== Results: empty and error ====================

    @Test
    fun anEmptyOrFailedQueryExplainsItselfInFullyVisibleText() {
        val created = launchWithLocalData(TagSearchResultsActivity::class.java)
        controller = created
        val activity = created.get()

        ScreenTestSupport.awaitQuiet(resultsFragment(activity).model!!)
        for (message in listOf(
            "No tags could be found that matched your query.",
            "An error has occurred: The connection was interrupted while loading the next group of " +
                "tags. Please try again.",
        )) {
            resultsFragment(activity).model!!.statusText = message
            idle()
            val text = activity.findViewById<TextView>(R.id.statusTextView)
            assertDisplayed("statusTextView", text)
            assertEquals(message, text.text.toString())
            assertTrue(
                "the whole explanation is laid out",
                text.layout.height <= text.height - text.compoundPaddingTop - text.compoundPaddingBottom,
            )
            for (line in 0 until text.layout.lineCount) {
                assertEquals("and none of it is ellipsized", 0, text.layout.getEllipsisCount(line))
            }
        }
    }

    // ==================== Results: pagination against the real transport ====================

    /**
     * Reaching the end of the list asks the catalog for the next page, and that page renders.
     *
     * Nothing here sets `isLoading` or assigns rows: the request is production's, held pending at
     * the transport so the loader can be read while it is genuinely outstanding, and the rows are
     * whatever production parsed out of the response. This is the part of the deleted
     * `BarberPoleQueryRegressionTest` that a hand-assigned model could not cover — if pagination
     * stopped asking for the next page, the second request would simply never arrive.
     */
    @Test
    fun reachingTheEndOfTheListRequestsAndRendersTheNextPage() {
        available = 26
        script(pageSize) // hold the second page until this test releases it
        ScreenTestSupport.withTransport(::respond) {
            val activity = launchResults(query = "quartet pagination", parts = 4)
            val model = resultsFragment(activity).model!!
            ScreenTestSupport.await("the first page to arrive") { !model.isLoading && model.tags.size == pageSize }

            assertEquals("only the first page has been asked for: $requested", 1, requested.size)
            val first = requested[0].toString()
            assertTrue("the request carries the query: $first", first.contains("q=quartet%20pagination"))
            assertTrue("and the part filter: $first", first.contains("Parts=4"))
            assertTrue("and starts at the first row: $first", first.contains("start=1"))
            assertTrue("there is more of the catalog to come", model.hasMoreResults)
            assertEquals(pageSize, activity.list().adapter.count)
            assertFalse("no page is outstanding yet", activity.pole().loading)

            // Scrolling to the end is the only thing that asks for the next page.
            activity.list().setSelection(pageSize - 1)
            ScreenTestSupport.await("the next page to be requested") { requested.size >= 2 }
            val second = requested[1].toString()
            assertTrue("the second request starts after the first page: $second", second.contains("start=${pageSize + 1}"))
            assertTrue("the query is carried into the next page: $second", second.contains("q=quartet%20pagination"))
            assertTrue("a pending next page marks the loader as loading", activity.pole().loading)
            assertTrue("and shows it", activity.pole().isShown)
            assertEquals("while the rows already fetched stay put", pageSize, activity.list().adapter.count)

            script(pageSize).put(true)
            ScreenTestSupport.await("the next page to arrive") { !model.isLoading && model.tags.size == available }

            assertFalse("the loader stops when the page arrives", activity.pole().loading)
            assertFalse("and goes away", activity.pole().isShown)
            assertFalse("the catalog is exhausted", model.hasMoreResults)
            assertEquals(available, activity.list().adapter.count)

            activity.list().setSelection(available - 1)
            idle()
            assertEquals("the last row can be reached", available - 1, activity.list().lastVisiblePosition)
            val last = activity.list().getChildAt(available - 1 - activity.list().firstVisiblePosition)
            assertEquals(
                "and it is a row from the second page",
                title(available - 1),
                last.findViewById<TextView>(R.id.titleTextView).text.toString(),
            )
            ScreenTestSupport.assertFullyVisible("the final row", last)
        }
    }

    /**
     * A page that fails explains itself, keeps what was already fetched, and its retry succeeds.
     *
     * The list asks for the failed page again as soon as it lays out at its end once more, so the
     * retry is production's own; the test only decides when that attempt is answered. The refresh
     * action the screen offers is then driven directly, once nothing is in flight, to show the
     * retry a reader can reach is wired to a real request too.
     */
    @Test
    fun aFailedPageExplainsItselfAndItsRetrySucceeds() {
        available = 26
        script(pageSize)
        ScreenTestSupport.withTransport(::respond) {
            val activity = launchResults(query = "quartet retry")
            val fragment = resultsFragment(activity)
            val model = fragment.model!!
            ScreenTestSupport.await("the first page to arrive") { !model.isLoading && model.tags.size == pageSize }

            activity.list().setSelection(pageSize - 1)
            ScreenTestSupport.await("the next page to be requested") { requested.size >= 2 }
            assertTrue("the page is genuinely outstanding", activity.pole().loading)
            script(pageSize).put(false)
            ScreenTestSupport.await("the failed page to report itself") { model.statusText != null }

            assertTrue(
                "the failure explains itself: ${model.statusText}",
                model.statusText!!.startsWith("An error has occurred: The connection was interrupted"),
            )
            val status = activity.findViewById<TextView>(R.id.statusTextView)
            assertDisplayed("statusTextView", status)
            ScreenTestSupport.assertFullyVisible("the failure message", status)
            assertEquals(model.statusText, status.text.toString())
            assertEquals("the rows already fetched survive a failed page", pageSize, activity.list().adapter.count)

            ScreenTestSupport.await("the failed page to be asked for again") { requested.size >= 3 }
            assertTrue(
                "the retry starts where the failed page did: ${requested[2]}",
                requested[2].toString().contains("start=${pageSize + 1}"),
            )
            script(pageSize).put(true)
            ScreenTestSupport.await("the retry to succeed") { !model.isLoading && model.tags.size == available }

            assertNull("a successful retry clears the failure", model.statusText)
            assertEquals("and the explanation goes away", View.GONE, status.visibility)
            assertEquals(available, activity.list().adapter.count)
            assertFalse("the loader stops", activity.pole().loading)
        }
    }

    /**
     * The retry a reader can reach: the screen's refresh action asks the catalog again.
     *
     * Nothing is in flight at that point, so the request this produces is unambiguously the
     * action's own.
     */
    @Test
    fun theRefreshActionAsksTheCatalogAgain() {
        available = 26
        script(pageSize) // the second page never completes, so the first page stays settled
        ScreenTestSupport.withTransport(::respond) {
            val activity = launchResults(query = "quartet refresh")
            val fragment = resultsFragment(activity)
            val model = fragment.model!!
            ScreenTestSupport.await("the first page to arrive") { !model.isLoading && model.tags.size == pageSize }
            assertEquals("nothing else has been asked for: $requested", 1, requested.size)

            retryThrough(activity, fragment)
            ScreenTestSupport.await("the refresh action to re-request the query") {
                requested.size >= 2 && !model.isLoading && model.tags.size == pageSize
            }

            val again = requested[1].toString()
            assertTrue("the refresh action starts a fresh first page: $again", again.contains("start=1"))
            assertTrue("and carries the same query: $again", again.contains("q=quartet%20refresh"))
            assertEquals("and the screen is populated again", pageSize, activity.list().adapter.count)
            assertNull("with nothing to explain", model.statusText)
        }
    }

    /**
     * A browse page that scrolls off screen stops its own pole; the visible one keeps loading.
     *
     * Both queries are genuinely outstanding throughout — nothing completes them — so the only
     * thing that can stop the offscreen pole is the lifecycle gate the deleted regression test
     * covered.
     */
    @Test
    fun anOffscreenBrowsePageStopsItsPoleWhileTheVisibleOneKeepsLoading() {
        script(0) // hold every browse page's first request open
        ScreenTestSupport.withTransport(::respond) {
            val created = ScreenTestSupport.build(TagBrowserActivity::class.java)
            ScreenTestSupport.prepareQueryModels(created.get()) { it.resultSetSize = pageSize }
            created.setup().visible()
            idle()
            controller = created
            val activity = created.get()
            val firstPole = browsePole(activity, 0)
            ScreenTestSupport.await("the visible browse page to start loading") {
                firstPole.loading && firstPole.hostResumed
            }
            // Robolectric never produces a frame on its own, and the view re-evaluates its motion
            // from its own pre-draw listener, so dispatch the frame callback a device would.
            firstPole.viewTreeObserver.dispatchOnPreDraw()
            assertTrue(
                "a visible pending pole moves: " + poleState(activity, firstPole),
                firstPole.isAnimating,
            )

            activity.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(1, false)
            ScreenTestSupport.await("the first page to go off screen") { !firstPole.hostResumed }

            firstPole.viewTreeObserver.dispatchOnPreDraw()
            assertFalse("an offscreen pole stops moving", firstPole.isAnimating)
            assertTrue("even though its own query is still pending", firstPole.loading)

            val secondPole = browsePole(activity, 1)
            ScreenTestSupport.await("the newly visible page to load") { secondPole.loading && secondPole.hostResumed }
            secondPole.viewTreeObserver.dispatchOnPreDraw()
            assertTrue(
                "the visible page keeps its pole running: " + poleState(activity, secondPole),
                secondPole.isAnimating,
            )
            assertTrue("and its query is outstanding too", requested.size >= 2)
        }
    }

    private fun poleState(
        activity: TagBrowserActivity,
        pole: BarberPoleLoadingView,
    ) = "loading=${pole.loading} host=${pole.hostResumed} attached=${pole.isAttachedToWindow} " +
        "shown=${pole.isShown} visibility=${pole.visibility} window=${pole.windowVisibility} " +
        "size=${pole.width}x${pole.height} rect=${pole.getGlobalVisibleRect(android.graphics.Rect())} " +
        "scale=${android.provider.Settings.Global.getFloat(
            activity.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )} animators=${android.animation.ValueAnimator.areAnimatorsEnabled()}"

    private fun browsePole(
        activity: TagBrowserActivity,
        position: Int,
    ): BarberPoleLoadingView {
        ScreenTestSupport.await("browse page $position to exist") {
            (activity.supportFragmentManager.findFragmentByTag("f$position") as? TagQueryFragment)?.view != null
        }
        val fragment = activity.supportFragmentManager.findFragmentByTag("f$position") as TagQueryFragment
        return fragment.requireView().findViewById(R.id.loadingProgressBar)
    }
}
