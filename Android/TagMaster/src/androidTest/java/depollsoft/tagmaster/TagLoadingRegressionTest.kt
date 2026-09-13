package depollsoft.tagmaster

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import bolts.TaskCompletionSource
import com.google.android.material.tabs.TabLayout
import depollsoft.tagmaster.barbershop.Tag
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Holds actual production requests pending, without sleeps, cache mutations or production delays. */
@androidx.test.filters.SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class TagLoadingRegressionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as Application
    private val requests = LinkedBlockingQueue<Pair<Int, TaskCompletionSource<Tag>>>()
    private val owners = java.util.concurrent.ConcurrentHashMap<TaskCompletionSource<Tag>, TagDetailActivity>()
    private val tagId = 2147483018
    private val callbacks =
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPreCreated(
                activity: Activity,
                state: Bundle?,
            ) {
                if (activity is TagDetailActivity) {
                    if (InstrumentationRegistry.getArguments().getString("captureOrientation") == "landscape") {
                        activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                    activity.tagLoader = { id, _ ->
                        TaskCompletionSource<Tag>()
                            .also {
                                owners[it] = activity
                                requests.add(id to it)
                            }.task
                    }
                }
            }

            override fun onActivityCreated(
                a: Activity,
                b: Bundle?,
            ) {}

            override fun onActivityStarted(a: Activity) {}

            override fun onActivityResumed(a: Activity) {}

            override fun onActivityPaused(a: Activity) {}

            override fun onActivityStopped(a: Activity) {}

            override fun onActivitySaveInstanceState(
                a: Activity,
                b: Bundle,
            ) {}

            override fun onActivityDestroyed(a: Activity) {}
        }

    @Before fun register() {
        // Connect before launch: UiAutomation can change rotation/configuration on connection.
        val automation = instrumentation.uiAutomation
        if (InstrumentationRegistry.getArguments().getString("captureOrientation") == "landscape") {
            automation.setRotation(android.app.UiAutomation.ROTATION_FREEZE_90)
        }
        instrumentation.waitForIdleSync()
        app.registerActivityLifecycleCallbacks(callbacks)
    }

    @After fun unregister() {
        app.unregisterActivityLifecycleCallbacks(callbacks)
        if (InstrumentationRegistry.getArguments().getString("captureOrientation") != null) {
            instrumentation.uiAutomation.setRotation(android.app.UiAutomation.ROTATION_UNFREEZE)
        }
    }

    private fun launch() =
        ActivityScenario.launch<TagDetailActivity>(
            Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, tagId),
        )

    private fun next() = requests.poll(5, TimeUnit.SECONDS) ?: error("Expected controlled request")

    private fun nextFor(scenario: ActivityScenario<TagDetailActivity>): Pair<Int, TaskCompletionSource<Tag>> {
        lateinit var current: TagDetailActivity
        scenario.onActivity { current = it }
        // Rotation can recreate the activity. Complete only the request owning the captured screen.
        while (true) {
            val request = next()
            if (owners[request.second] === current) return request
            request.second.setCancelled()
        }
    }

    private fun tag(
        id: Int = tagId,
        title: String = "When the quartet gathers",
    ) = Tag().apply {
        this.id = id
        this.title = title
        parts = 4
        writtenKey = "C"
        tagType = "Barbershop"
        arranger = "Fixture arranger"
        rating = 4.0
    }

    private fun complete(
        request: Pair<Int, TaskCompletionSource<Tag>>,
        result: Tag = tag(request.first),
    ) {
        instrumentation.runOnMainSync { request.second.setResult(result) }
        instrumentation.waitForIdleSync()
    }

    private fun state(
        a: TagDetailActivity,
        loading: Boolean,
        loaded: Boolean,
        failed: Boolean = false,
    ) {
        assertEquals(loading, a.isLoading)
        assertEquals(failed, a.loadFailed)

        fun shown(
            id: Int,
            expected: Boolean,
        ) = assertEquals("View $id", if (expected) View.VISIBLE else View.GONE, a.findViewById<View>(id).visibility)
        shown(R.id.detailLoadingState, loading && !loaded)
        shown(R.id.viewPager, loaded)
        shown(R.id.tabLayout, loaded)
        shown(R.id.imageView1, loaded)
        shown(R.id.progress, loading && loaded)
        shown(R.id.detailErrorState, failed && !loaded)
        if (!loaded) {
            assertNull(a.findViewById<ViewPager2>(R.id.viewPager).adapter)
            assertNull(a.findViewById<View>(R.id.tagIdTextView))
        }
        val menu = MenuBuilder(a)
        assertEquals(loaded, a.onCreateOptionsMenu(menu))
        if (loaded) {
            a.onPrepareOptionsMenu(menu)
            assertEquals(!loading, menu.findItem(R.id.refreshMenuItem).isEnabled)
        }
        if (!loading || loaded) assertFalse(a.findViewById<TagLoadingView>(R.id.quartetIllustration).isAnimating)
    }

    private fun slots(a: TagDetailActivity) {
        val tabs = a.findViewById<TabLayout>(R.id.tabLayout)
        assertEquals(TabLayout.MODE_FIXED, tabs.tabMode)
        assertEquals("No max-width padding", 0, tabs.paddingLeft + tabs.paddingRight)
        val strip = tabs.getChildAt(0) as ViewGroup
        assertEquals(tabs.width, strip.width)
        var right = 0
        for (i in 0 until 4) {
            val child = strip.getChildAt(i)
            assertEquals(
                "slot=$i, bar=${tabs.width}, strip=${strip.width}, gravity=${tabs.tabGravity}, child=${child.width}, weight=${(child.layoutParams as android.widget.LinearLayout.LayoutParams).weight}",
                right,
                child.left,
            )
            assertTrue(kotlin.math.abs(child.width * 4 - strip.width) <= 4)
            val text = tabs.getTabAt(i)!!.customView!!.findViewById<TextView>(android.R.id.text1)
            assertEquals(0, text.layout.getEllipsisCount(text.lineCount - 1))
            assertTrue("Label height fits", text.layout.height <= text.height - text.compoundPaddingTop - text.compoundPaddingBottom)
            right = child.right
        }
        assertEquals(strip.width, right)
    }

    private fun capture(suffix: String) {
        val label = InstrumentationRegistry.getArguments().getString("captureLabel") ?: return
        // Wait for a real rendered frame, not a timer or a synthetic loading delay.
        val committed = java.util.concurrent.CountDownLatch(1)
        instrumentation.runOnMainSync {
            val activity =
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                    .getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .single()
            activity.window.decorView.viewTreeObserver
                .registerFrameCommitCallback { committed.countDown() }
            activity.window.decorView.invalidate()
        }
        assertTrue("Capture frame committed", committed.await(5, TimeUnit.SECONDS))
        // PixelCopy captures the actual app window without UiAutomation's screenshot
        // binder, which can stall on a long-running emulator. No reconstructed layout.
        val copied = java.util.concurrent.CountDownLatch(1)
        lateinit var bitmap: Bitmap
        var result = android.view.PixelCopy.ERROR_UNKNOWN
        instrumentation.runOnMainSync {
            val activity =
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                    .getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .single()
            bitmap = Bitmap.createBitmap(activity.window.decorView.width, activity.window.decorView.height, Bitmap.Config.ARGB_8888)
            android.view.PixelCopy.request(activity.window, bitmap, {
                result = it
                copied.countDown()
            }, android.os.Handler(android.os.Looper.getMainLooper()))
        }
        assertTrue("Native window copied", copied.await(5, TimeUnit.SECONDS))
        assertEquals(android.view.PixelCopy.SUCCESS, result)
        val scale = minOf(1f, 800f / maxOf(bitmap.width, bitmap.height))
        val small = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        val file = File(app.getExternalFilesDir(null), "tagmaster-android-loading-$label-$suffix.jpg")
        file.outputStream().use { small.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        if (small !== bitmap) small.recycle()
        bitmap.recycle()
    }

    private fun exportQuartetFrames() {
        instrumentation.runOnMainSync {
            val directory = File(app.getExternalFilesDir(null), "quartet-frames").apply { mkdirs() }
            for (dark in listOf(false, true)) {
                val config = android.content.res.Configuration(app.resources.configuration)
                config.uiMode = (
                    config.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                            .inv()
                ) or
                    (if (dark) android.content.res.Configuration.UI_MODE_NIGHT_YES else android.content.res.Configuration.UI_MODE_NIGHT_NO)
                val view = TagLoadingView(app.createConfigurationContext(config))
                for (scale in listOf(1, 10)) {
                    view.layout(0, 0, 216 * scale, 96 * scale)
                    for ((label, phase) in listOf(
                        "0" to 0f,
                        "0.125" to 0.125f,
                        "0.25" to 0.25f,
                        "0.5" to 0.5f,
                        "0.75" to 0.75f,
                        "1" to 1f,
                        "still" to 0f,
                    )) {
                        val bitmap =
                            Bitmap.createBitmap(
                                view.width,
                                view.height,
                                Bitmap.Config.ARGB_8888,
                                true,
                                android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB),
                            )
                        view.drawQuartet(android.graphics.Canvas(bitmap), phase, label != "still")
                        File(directory, "android-${if (dark) "dark" else "light"}-$scale-$label.png").outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                        bitmap.recycle()
                    }
                }
            }
        }
    }

    @Test fun controlled_pending_to_loaded_capture() {
        launch().use { scenario ->
            scenario.onActivity { a ->
                state(a, true, false)
                assertEquals("Loading tag $tagId…", a.findViewById<TextView>(R.id.detailLoadingStatus).text.toString())
                val view = a.findViewById<TagLoadingView>(R.id.quartetIllustration)
                assertEquals(android.animation.ValueAnimator.areAnimatorsEnabled(), view.isAnimating)
                assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, view.importantForAccessibility)
            }
            exportQuartetFrames()
            capture("pending")
            complete(nextFor(scenario))
            capture("loaded")
            scenario.onActivity { a ->
                state(a, false, true)
                assertEquals(tagId, a.tag!!.id)
                slots(a)
                assertEquals(tagId.toString(), a.findViewById<TextView>(R.id.tagIdTextView).text.toString())
            }
        }
    }

    @Test fun failure_retry_and_failed_refresh_keep_useful_content() {
        launch().use { scenario ->
            val first = next()
            instrumentation.runOnMainSync { first.second.setError(IllegalStateException("offline")) }
            scenario.onActivity { a ->
                state(a, false, false, true)
                a.findViewById<View>(R.id.detailRetryButton).performClick()
                state(a, true, false)
            }
            complete(next())
            scenario.onActivity { a ->
                val menu = MenuBuilder(a)
                a.onCreateOptionsMenu(menu)
                a.onOptionsItemSelected(menu.findItem(R.id.refreshMenuItem))
                state(a, true, true)
            }
            val refresh = next()
            instrumentation.runOnMainSync { refresh.second.setError(IllegalStateException("offline")) }
            scenario.onActivity { a ->
                state(a, false, true, true)
                assertEquals(tagId, a.tag!!.id)
                a.findViewById<View>(com.google.android.material.R.id.snackbar_action).performClick()
            }
            complete(next(), tag(title = "Quartet refreshed"))
            scenario.onActivity { a ->
                state(a, false, true)
                assertEquals("Quartet refreshed", a.findViewById<TextView>(R.id.titleTextView).text.toString())
            }
        }
    }

    @Test fun pending_background_hidden_and_detached_stop_motion() {
        val scenario = launch()
        val request = next()
        lateinit var illustration: TagLoadingView
        scenario.onActivity { a ->
            illustration = a.findViewById(R.id.quartetIllustration)
            a.findViewById<View>(R.id.detailLoadingState).visibility = View.GONE
            assertFalse(illustration.isAnimating)
            a.findViewById<View>(R.id.detailLoadingState).visibility = View.VISIBLE
            assertEquals(android.animation.ValueAnimator.areAnimatorsEnabled(), illustration.isAnimating)
        }
        scenario.moveToState(Lifecycle.State.CREATED)
        assertFalse(illustration.isAnimating)
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity { assertEquals(android.animation.ValueAnimator.areAnimatorsEnabled(), illustration.isAnimating) }
        scenario.close()
        complete(request)
        assertFalse(illustration.isAnimating)
    }

    @Test fun old_screen_completion_does_not_populate_next_screen() {
        val first = launch()
        val old = next()
        first.close()
        launch().use { current ->
            val request = next()
            complete(old)
            current.onActivity { state(it, true, false) }
            complete(request)
            current.onActivity { state(it, false, true) }
        }
    }
}
