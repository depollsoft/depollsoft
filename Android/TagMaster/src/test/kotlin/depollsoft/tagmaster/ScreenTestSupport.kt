package depollsoft.tagmaster

import android.app.Activity
import android.app.Application
import android.graphics.Rect
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import bolts.Task
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.util.Preferences
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.barbershop.Video
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import java.io.File

/**
 * Shared helpers for the Robolectric screen tests migrated from `src/androidTest`.
 *
 * These stand in for two things the instrumented suite needed a device for: Espresso's polling
 * (Robolectric's looper is paused and deterministic, so `idle()` is exact) and its `isDisplayed()`
 * matcher (Robolectric lays activities out for real, so the same three conditions read directly).
 */
internal object ScreenTestSupport {
    /** The instrumented `NavigationTestFixture` tag, kept id-for-id so the cases still line up. */
    const val FIXTURE_TAG_ID = 2147483017

    fun idle() = shadowOf(Looper.getMainLooper()).idle()

    fun isDisplayed(view: View?): Boolean =
        view != null && view.isShown && view.width > 0 && view.height > 0 &&
            view.getGlobalVisibleRect(Rect())

    fun assertDisplayed(
        name: String,
        view: View?,
    ) {
        assertNotNull("$name should exist", view)
        assertTrue("$name should be displayed", isDisplayed(view))
    }

    /**
     * Espresso's `fullyVisible`: no part of the view is clipped away by an ancestor.
     *
     * [isDisplayed] only requires a non-empty intersection, which a one-pixel sliver satisfies.
     * This is the instrumented `LayoutRegressionTest` check restored unchanged: the visible
     * portion in the view's own drawing coordinates has to be the whole view.
     */
    fun isFullyVisible(view: View): Boolean {
        val visible = Rect()
        // Local drawing coordinates include the view's own scroll offset. Native centered dialog
        // TextViews can have a large horizontal text scroll value.
        val viewport = Rect(view.scrollX, view.scrollY, view.scrollX + view.width, view.scrollY + view.height)
        return view.isShown && view.width > 0 && view.height > 0 &&
            view.getLocalVisibleRect(visible) && visible == viewport
    }

    fun assertFullyVisible(
        name: String,
        view: View,
    ) = assertTrue(
        "$name should be wholly on screen, not merely intersecting it: ${fullBounds(view)}",
        isFullyVisible(view),
    )

    /** The view's own rectangle, in screen coordinates. */
    fun fullBounds(view: View): Rect =
        Rect(0, 0, view.width, view.height).also {
            val xy = IntArray(2)
            view.getLocationOnScreen(xy)
            it.offset(xy[0], xy[1])
        }

    /**
     * Intersect the view's bounds with the window and every clipping ancestor, as hit testing does.
     */
    fun reachableBounds(view: View): Rect {
        val bounds = fullBounds(view)
        val frame = Rect().also { view.getWindowVisibleDisplayFrame(it) }
        assertTrue("shown and inside the window: $bounds / $frame", view.isShown && bounds.intersect(frame))
        var parent = view.parent
        while (parent is android.view.ViewGroup) {
            if (parent.clipChildren) {
                val clip = fullBounds(parent)
                if (parent.clipToPadding) {
                    clip.left += parent.paddingLeft
                    clip.top += parent.paddingTop
                    clip.right -= parent.paddingRight
                    clip.bottom -= parent.paddingBottom
                }
                assertTrue("inside ${parent.javaClass.simpleName}: $bounds / $clip", bounds.intersect(clip))
            }
            parent = parent.parent
        }
        return bounds
    }

    /**
     * The instrumented `assertReachableButton`: a 48dp reachable target whose label fits inside it.
     */
    fun assertTouchTarget(button: android.widget.TextView): Rect {
        assertTrue(
            "'${button.text}' should lay its label out",
            button.layout != null &&
                button.layout.height <= button.height - button.compoundPaddingTop - button.compoundPaddingBottom,
        )
        for (line in 0 until button.layout.lineCount) {
            org.junit.Assert.assertEquals(
                "'${button.text}' should not ellipsize",
                0,
                button.layout.getEllipsisCount(line),
            )
        }
        val bounds = reachableBounds(button)
        val minimum = (48 * button.resources.displayMetrics.density).toInt()
        assertTrue(
            "'${button.text}' should keep a 48dp reachable target: $bounds",
            bounds.width() >= minimum && bounds.height() >= minimum,
        )
        val label =
            fullBounds(button).apply {
                left += button.compoundPaddingLeft
                right -= button.compoundPaddingRight
                top += button.totalPaddingTop
                bottom -= button.totalPaddingBottom
            }
        assertTrue("'${button.text}' should keep its whole label reachable: $label / $bounds", bounds.contains(label))
        return bounds
    }

    /** Espresso's `scrollTo()`: bring the view inside its scrolling ancestor, then settle. */
    fun scrollTo(view: View): View {
        view.requestRectangleOnScreen(Rect(0, 0, view.width, view.height), true)
        idle()
        return view
    }

    /**
     * Where the installed stream handler asks for a response body, shared by every sandbox.
     *
     * `URL.setURLStreamHandlerFactory` can only be called once per JVM, and Robolectric loads this
     * class again in every sandbox, so the factory that wins belongs to whichever test class
     * happened to run first — a plain static here would be invisible to all the others. The system
     * properties table is owned by the bootstrap loader and shared by every sandbox, and
     * `java.util.function.Function` is a `java.*` interface that all of them resolve to the same
     * type, so the winning handler can still call back into the sandbox running the current test.
     */
    private const val TRANSPORT_KEY = "depollsoft.tagmaster.screentest.transport"

    private fun transport(): java.util.function.Function<java.net.URL, java.io.InputStream>? {
        @Suppress("UNCHECKED_CAST")
        return System.getProperties()[TRANSPORT_KEY]
            as? java.util.function.Function<java.net.URL, java.io.InputStream>
    }

    /**
     * Refuse every outbound HTTP request for the whole test JVM unless a test supplies a transport.
     *
     * `TagQueryFragment.refresh()` issues a real `Tag.query` from `onCreate`, so on a machine with
     * a network the browse and results screens were quietly appending live catalog rows on top of
     * the fixtures — which is exactly how the first version of these tests came out flaky
     * (12 expected, 32 observed). The instrumented suite held the same boundary with its own
     * `URLStreamHandlerFactory`; this one closes it, and [withTransport] reopens it onto canned
     * responses the way the instrumented `BarberPoleQueryRegressionTest` did.
     * `setURLStreamHandlerFactory` is once-per-JVM and `java.net.URL` is shared across
     * Robolectric's per-class sandboxes, so a second install throws and is ignored.
     */
    fun blockNetwork() {
        try {
            java.net.URL.setURLStreamHandlerFactory { protocol ->
                if (protocol != "http" && protocol != "https") {
                    null
                } else {
                    object : java.net.URLStreamHandler() {
                        override fun openConnection(url: java.net.URL): java.net.URLConnection =
                            object : java.net.URLConnection(url) {
                                override fun connect() = Unit

                                override fun getInputStream(): java.io.InputStream {
                                    val responder =
                                        transport()
                                            ?: throw java.io.IOException(
                                                "Unit tests do not reach the network: $url",
                                            )
                                    return responder.apply(url)
                                }
                            }
                    }
                }
            }
        } catch (_: Error) {
            // Already installed by an earlier test class in this JVM.
        }
    }

    /**
     * Answer every catalog request inside [block] with [respond], on the real request thread.
     *
     * This is the JVM counterpart of the instrumented suite's process-local
     * `URLStreamHandlerFactory`: production code builds its own URL, opens its own stream and
     * parses its own XML, and only the bytes on the wire are supplied by the test.
     */
    fun <T> withTransport(
        respond: (java.net.URL) -> java.io.InputStream,
        block: () -> T,
    ): T {
        blockNetwork()
        val properties = System.getProperties()
        val previous = properties[TRANSPORT_KEY]
        properties[TRANSPORT_KEY] =
            object : java.util.function.Function<java.net.URL, java.io.InputStream> {
                override fun apply(url: java.net.URL): java.io.InputStream = respond(url)
            }
        return try {
            block()
        } finally {
            if (previous == null) properties.remove(TRANSPORT_KEY) else properties[TRANSPORT_KEY] = previous
        }
    }

    /**
     * Drain the main looper until [condition] holds, failing at [timeoutMs].
     *
     * `Tag.loadTagById` reads its disk cache on Bolts' background executor and reports the result
     * back through a main-thread post, so a single `idle()` after `setup()` can run before that
     * read has even started. This is a bounded wait on the real condition, never a fixed delay.
     */
    fun await(
        description: String,
        timeoutMs: Long = 10_000,
        condition: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            idle()
            if (condition()) return
            assertTrue("timed out waiting for $description", System.currentTimeMillis() < deadline)
            Thread.yield()
        }
    }

    /**
     * Wait for a detail screen to finish the load it started, and prove it came from the cache.
     *
     * With the network refused, a disk-cache miss falls through to `loadTagById(id, refresh=true)`
     * and fails, so `loadFailed == false` together with the expected tag is exactly the
     * "resolved from the cache, no network" claim the instrumented fixture made.
     */
    fun awaitTagLoaded(activity: TagDetailActivity): Tag {
        await("the detail screen for tag ${activity.tagId} to settle") {
            !activity.isLoading && (activity.tag != null || activity.loadFailed)
        }
        org.junit.Assert.assertFalse(
            "the cached tag should resolve without reaching the network",
            activity.loadFailed,
        )
        return requireNotNull(activity.tag) { "the detail screen should have a tag" }
    }

    /**
     * Point the app-wide singletons at the test application and keep every list write in memory.
     *
     * `RichApplication.getAppContext()` is populated by `RichApplication.onCreate`, which these
     * tests do not run because the module's `robolectric.properties` uses a plain `Application` to
     * keep Firebase and account startup out of unit tests. `Tag`'s disk cache reads that context,
     * so it is set directly, exactly as the existing `TabletListDetailTest` does.
     */
    fun startClean() {
        blockNetwork()

        RichApplication::class.java
            .getDeclaredField("context")
            .apply { isAccessible = true }
            .set(null, RuntimeEnvironment.getApplication())
        // Keep every list write in memory, but never empty the store: `preference(key, default)`
        // registers its default once, when the owning object is first touched, and Robolectric
        // shares statics between test classes with the same configuration. Clearing here would
        // strand a later class reading a default that will never be registered again.
        // Preferences are deliberately left in whatever mode the suite is already using. These
        // screens only need saved-list writes suppressed, and switching the global preference mode
        // strands other classes: `preference(key, default)` registers its default once, into
        // whichever store was selected at that moment, and never registers it again.
        rebindPreferences()
        seedSettingsDefaults()
        ListModel.setTestMode(true)
        FavoritesModel.favoriteIds = com.bindroid.trackable.TrackableCollection()
        TeachableTagsModel.teachableTagIds = com.bindroid.trackable.TrackableCollection()
    }

    /**
     * Wait for a query that is already in flight to finish, then stop the model asking for more.
     *
     * The browse and results screens install their own `QueryModel` after `onFragmentPreCreated`
     * runs, so the model a test is handed may already have issued one request. With the network
     * blocked that request fails at once, but it fails on Bolts' background executor and reports
     * back through a main-thread post, so the test has to join it rather than assume it is done.
     * This is a bounded wait on a condition, not a fixed delay.
     */
    fun awaitQuiet(model: QueryModel) {
        val deadline = System.currentTimeMillis() + 10_000
        while (model.isLoading && System.currentTimeMillis() < deadline) {
            idle()
            Thread.yield()
        }
        assertTrue("the initial query should settle", !model.isLoading)
        model.hasMoreResults = false
        model.statusText = null
        model.tags.clear()
        idle()
    }

    /**
     * Make `Preferences` bind to the Application of whichever test runs next.
     *
     * `ensureInitialized()` latches a static `initialized` flag and keeps the `SharedPreferences`
     * of the first context it ever saw. Robolectric hands every test a fresh Application, so
     * whichever class touches Preferences first otherwise leaves the rest reading a store that
     * belongs to a context which no longer exists — which is exactly how `SettingsLoginStateTest`
     * began passing on some runs and failing on others once these screens changed the order.
     * `TabletListDetailTest` already resets this flag for the same reason; clearing it on the way
     * in and on the way out makes the ordering stop mattering.
     */
    /**
     * Re-register the settings the screens read, whatever store is currently selected.
     *
     * `preference(key, default)` registers its default exactly once, when SettingsModel is first
     * touched, and into whichever store test mode had selected then. A later class that empties or
     * swaps that store leaves those delegates returning null, so a plain settings read throws.
     * Writing the production defaults back makes the ordering stop mattering.
     */
    fun seedSettingsDefaults() {
        SettingsModel.minimumRandomDownloads = 100
        SettingsModel.minimumRandomTagRating = 2.5
        SettingsModel.randomLearningTracksFilter = null
        SettingsModel.randomSheetMusicFilter = true
        SettingsModel.wakeLockOnSheetMusic = true
    }

    private fun rebindPreferences() {
        Preferences::class.java
            .getDeclaredField("initialized")
            .apply { isAccessible = true }
            .setBoolean(null, false)
    }

    fun finish(controller: ActivityController<*>?) {
        idle()
        controller?.close()
        idle()
        // Deliberately no rebind here: forcing a re-bind on the way out would hand the next class
        // a brand-new, empty SharedPreferences and strand its settings reads. Binding is corrected
        // on the way in instead.
        ListModel.setTestMode(false)
    }

    /** Build an activity themed and laid out the way the launcher does, without starting it yet. */
    fun <A : Activity> build(
        clazz: Class<A>,
        intent: android.content.Intent? = null,
    ): ActivityController<A> {
        val controller =
            if (intent == null) Robolectric.buildActivity(clazz) else Robolectric.buildActivity(clazz, intent)
        controller.get().setTheme(R.style.AppTheme)
        return controller
    }

    fun <A : Activity> launch(
        clazz: Class<A>,
        intent: android.content.Intent? = null,
    ): ActivityController<A> = build(clazz, intent).also { it.setup(); idle() }

    /**
     * Dismiss the first-run changelog, which `MeActivity` shows from `onCreate`.
     *
     * The instrumented suite tapped its button through Espresso inside a swallowed
     * `NoMatchingViewException`. Robolectric exposes the dialog directly, so this is unconditional
     * and cannot silently skip.
     */
    fun dismissChangelog() {
        org.robolectric.shadows.ShadowDialog.getLatestDialog()?.takeIf { it.isShowing }?.dismiss()
        idle()
    }

    /**
     * Choose [label] in one of the search form's Material dropdowns.
     *
     * `TagSearchActivity` wires each dropdown with `setOnItemClickListener`, and that listener is
     * what the popup invokes on a tap. Calling it drives the production handler; only the popup
     * window itself — which has no behaviour of its own — is skipped.
     */
    fun chooseDropdown(
        activity: Activity,
        viewId: Int,
        arrayId: Int,
        label: String,
    ) {
        val dropdown = activity.findViewById<MaterialAutoCompleteTextView>(viewId)
        val choices = activity.resources.getStringArray(arrayId)
        val position = choices.indexOf(label)
        assertTrue("'$label' should be one of ${choices.toList()}", position >= 0)
        dropdown.setText(choices[position], false)
        dropdown.onItemClickListener!!.onItemClick(
            null as AdapterView<*>?,
            dropdown,
            position,
            position.toLong(),
        )
        idle()
    }

    /** The fully-populated tag the instrumented `NavigationTestFixture` wrote to the disk cache. */
    fun fixtureTag(): Tag =
        Tag().apply {
            id = FIXTURE_TAG_ID
            title = "Navigation fixture"
            writtenKey = "C"
            parts = 4
            arranger = "Fixture arranger"
            yearArranged = "2026"
            classicTagNumber = 17
            rating = 4.0
            downloadCount = 123
            recordingMethod = "Separate parts for navigation coverage"
            allPartsTrackUri = track("all")
            tenorTrackUri = track("tenor")
            leadTrackUri = track("lead")
            baritoneTrackUri = track("baritone")
            bassTrackUri = track("bass")
            videos =
                mutableListOf(
                    Video().apply {
                        id = 1
                        sungBy = "Fixture quartet"
                        sungKey = "C"
                        youTubeCode = "navigation-fixture"
                    },
                )
        }

    /** Selecting a part does not download or play it; each URI just distinguishes the binding. */
    fun track(part: String) =
        RemoteLocation().apply {
            uri = "https://example.invalid/navigation-$part.mp3"
            type = "mp3"
        }

    /**
     * Write [tag] where `Tag.loadTagById` looks first, so the real disk-cache path resolves it with
     * no network — the same thing the instrumented `NavigationTestFixture` did.
     */
    fun cacheOnDisk(tag: Tag) {
        val file = File(RuntimeEnvironment.getApplication().filesDir, "TagCache/${tag.id}")
        file.parentFile!!.mkdirs()
        file.writeText(JsonSerializer.serialize(tag).toString())
    }

    /**
     * Empty both halves of `Tag`'s cache: the disk directory and its in-memory `SparseArray`.
     *
     * Clearing only the files left the process-wide soft-reference map populated, so a test that
     * mutated the tag it was handed — `DetailMetadataGeometryTest` strips key and sheet data off
     * its fixture — handed that same mutated object to every later test that reused the id.
     * `Tag.clearCache()` is the production API for both halves; the recursive delete only removes
     * the now-empty directory the tests created.
     */
    fun clearTagCaches() {
        Tag.clearCache()
        File(RuntimeEnvironment.getApplication().filesDir, "TagCache").deleteRecursively()
    }

    /**
     * Hand every `TagQueryFragment` this activity creates a model prepared by [configure].
     *
     * `onFragmentPreCreated` runs before `TagQueryFragment.onCreate`, which is what issues the
     * first `Tag.query`, so this is the seam that decides what the screen asks the catalog for.
     * It has to be installed on the activity instance before `setup()`: the platform's
     * `onActivityPreCreated` only exists from API 29, and these screens are configured for 28.
     * The request itself is left to happen for real, against whatever [withTransport] answers.
     */
    fun prepareQueryModels(
        activity: AppCompatActivity,
        configure: (QueryModel) -> Unit,
    ) {
        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentPreCreated(
                    fm: androidx.fragment.app.FragmentManager,
                    f: androidx.fragment.app.Fragment,
                    state: Bundle?,
                ) {
                    if (f is TagQueryFragment) {
                        if (f.model == null) f.model = QueryModel()
                        configure(f.model!!)
                    }
                }
            },
            true,
        )
    }

    /**
     * Hold every tag request and every query off the network for the duration of [block].
     *
     * `TagDetailActivity` already exposes a `tagLoader` seam, and `TagQueryFragment` takes whatever
     * model it is handed, so both are supplied before `onCreate` through the platform's own
     * `ActivityLifecycleCallbacks` — the seam the instrumented `LayoutRegressionTest` used. No
     * production code is involved beyond those existing hooks.
     */
    fun <T> withLocalData(
        loader: (Int, Boolean) -> Task<Tag>,
        block: () -> T,
    ): T {
        val app = RuntimeEnvironment.getApplication() as Application
        val callbacks =
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityPreCreated(
                    activity: Activity,
                    state: Bundle?,
                ) {
                    if (activity is TagDetailActivity) activity.tagLoader = loader
                    if (activity is AppCompatActivity) {
                        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
                            object : androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                                override fun onFragmentPreCreated(
                                    fm: androidx.fragment.app.FragmentManager,
                                    f: androidx.fragment.app.Fragment,
                                    state: Bundle?,
                                ) {
                                    if (f is TagQueryFragment) {
                                        // Keep the real selected-mode model; no catalog traffic
                                        // until a test asks for it.
                                        if (f.model == null) f.model = QueryModel()
                                        f.model!!.hasMoreResults = false
                                    }
                                }
                            },
                            true,
                        )
                    }
                }

                override fun onActivityCreated(
                    a: Activity,
                    b: Bundle?,
                ) = Unit

                override fun onActivityStarted(a: Activity) = Unit

                override fun onActivityResumed(a: Activity) = Unit

                override fun onActivityPaused(a: Activity) = Unit

                override fun onActivityStopped(a: Activity) = Unit

                override fun onActivitySaveInstanceState(
                    a: Activity,
                    b: Bundle,
                ) = Unit

                override fun onActivityDestroyed(a: Activity) = Unit
            }
        app.registerActivityLifecycleCallbacks(callbacks)
        return try {
            block()
        } finally {
            app.unregisterActivityLifecycleCallbacks(callbacks)
        }
    }
}
