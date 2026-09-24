package depollsoft.tagmaster

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Looper
import bolts.Task
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.util.Preferences
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.barbershop.Video
import org.junit.Assert.assertTrue
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import java.io.File

/**
 * Shared helpers for the Robolectric screen tests: a clean model and preference state, the
 * network boundary, the tag disk cache, and activity launch. Robolectric's looper is paused and
 * deterministic, so `idle()` is exact.
 */
internal object ScreenTestSupport {
    /** The instrumented `NavigationTestFixture` tag, kept id-for-id so the cases still line up. */
    const val FIXTURE_TAG_ID = 2147483017

    fun idle() = shadowOf(Looper.getMainLooper()).idle()

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
     * `QueryModel.refresh()` issues a real `Tag.query` from `onCreate`, so on a machine with
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
                                    // The handler is JVM-wide, so Robolectric's own download of an
                                    // android-all jar it hasn't cached yet (the first class on a
                                    // new SDK level, on a fresh CI runner) comes through here too.
                                    // HttpClient doesn't use URL handlers, so it still reaches Maven.
                                    if (url.path.contains("/org/robolectric/")) return fetchArtifact(url)
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

    private fun fetchArtifact(url: java.net.URL): java.io.InputStream {
        val response =
            java.net.http.HttpClient
                .newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build()
                .send(
                    java.net.http.HttpRequest
                        .newBuilder(url.toURI())
                        .build(),
                    java.net.http.HttpResponse.BodyHandlers
                        .ofInputStream(),
                )
        if (response.statusCode() != 200) throw java.io.IOException("HTTP ${response.statusCode()} for $url")
        return response.body()
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
     *
     * No test in this module may call `FirebaseApp.initializeApp(context)`: that reads the real
     * `google-services.json`, and a PitchPerfect Robolectric run once uploaded a crash to the
     * *production* Crashlytics project that way. Tests that need auth or Firestore mock the
     * statics (see `SavedListReorderTest`); a test that genuinely needs a `FirebaseApp` must build
     * one from synthetic options and disable collection, as PitchPerfect's `ensureFirebaseApp`
     * does.
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
        FavoritesModel.favoriteIds = emptyList()
        TeachableTagsModel.teachableTagIds = emptyList()
        resetLists()
    }

    /**
     * Forget every user-defined list, in both halves of the store.
     *
     * `TagLists` keeps its registry in memory and in `Preferences`, and `ListModel` keeps the tags
     * of every list in a process-wide map that Robolectric shares between test classes. Resetting
     * only the registry would leave a list created by an earlier test to be rediscovered from its
     * stored tags, so the tags go first and the registry second.
     */
    fun resetLists() {
        ListModel.storedKeys().filter { TagLists.isCustom(it) }.forEach { ListModel.discard(it) }
        TagLists.resetForTest()
        Preferences.setAsync("tagmaster.listNames", null)
        Preferences.setAsync("tagmaster.listOrder", null)
    }

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

    /**
     * Make `Preferences` bind to the Application of whichever test runs next: it latches the
     * `SharedPreferences` of the first context it ever saw, and Robolectric hands every test a
     * fresh Application.
     */
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
        resetLists()
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
     * Hold every tag request off the network for the duration of [block].
     *
     * `TagDetailActivity` exposes a `tagLoader` seam, supplied before `onCreate` through the
     * platform's own `ActivityLifecycleCallbacks` — the seam the instrumented
     * `LayoutRegressionTest` used. Query screens stay off the network through [withTransport].
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
