package depollsoft.tagmaster

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import depollsoft.lib.json.JsonSerializer
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real QueryModel/Tag.query requests paused at URL.openStream in this isolated test process. */
@RunWith(AndroidJUnit4::class)
class BarberPoleQueryRegressionTest {
    companion object {
        private val requests = CopyOnWriteArrayList<Request>()

        private class Request(
            val url: URL,
        ) {
            val ready = CountDownLatch(1)
            var xml = "<tags count=\"0\" available=\"0\"/>"

            fun complete(response: String = xml) {
                xml = response
                ready.countDown()
            }
        }

        @BeforeClass @JvmStatic
        fun pendingNetwork() {
            // URL factories are process-local and set once. Run this class in its own instrumentation invocation.
            URL.setURLStreamHandlerFactory { protocol ->
                if (protocol != "https") {
                    null
                } else {
                    object : URLStreamHandler() {
                        override fun openConnection(url: URL): URLConnection =
                            object : URLConnection(url) {
                                override fun connect() = Unit

                                override fun getInputStream(): java.io.InputStream {
                                    check(url.host == "www.barbershoptags.com") { "Unexpected network host in isolated query fixture" }
                                    val request = Request(url).also { requests.add(it) }
                                    check(request.ready.await(45, TimeUnit.SECONDS)) { "Pending fixture timed out" }
                                    return ByteArrayInputStream(request.xml.toByteArray())
                                }
                            }
                    }
                }
            }
        }
    }

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val motionDisabled get() =
        android.provider.Settings.Global
            .getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) ==
            0f

    @Test fun pending_search_pagination_and_browse_offscreen_lifecycle() {
        val model =
            QueryModel().apply {
                query = "Barber pole fixture"
                parts = 4
            }
        val intent =
            Intent(context, TagSearchResultsActivity::class.java)
                .putExtra(TagQueryFragment.QUERY_MODEL, JsonSerializer.serialize(model).toString())
        try {
            launchPending<TagSearchResultsActivity>(intent).use { scenario ->
                var loader: BarberPoleLoadingView? = null
                waitUntil {
                    scenario.onActivity { loader = it.findViewById(R.id.loadingProgressBar) }
                    loader?.loading == true &&
                        requests.isNotEmpty()
                }
                waitUntil { loader!!.getGlobalVisibleRect(android.graphics.Rect()) }
                Thread.sleep(100)
                assertTrue(loader!!.isShown)
                assertEquals(
                    "Loader host=${loader!!.hostResumed} loading=${loader!!.loading} shown=${loader!!.isShown} attached=${loader!!.isAttachedToWindow} window=${loader!!.windowVisibility} bounds=${loader!!.getGlobalVisibleRect(
                        android.graphics.Rect(),
                    )} scale=${android.provider.Settings.Global.getString(
                        context.contentResolver,
                        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                    )}",
                    !motionDisabled,
                    loader!!.isAnimating,
                )
                assertTrue(
                    requests
                        .first()
                        .url
                        .toString()
                        .contains("q=Barber%20pole%20fixture"),
                )
                assertTrue(
                    requests
                        .first()
                        .url
                        .toString()
                        .contains("Parts=4"),
                )
                capture("search-pending")
                val first = draw(loader!!)
                Thread.sleep(500)
                val second = draw(loader!!)
                assertFrames(first, second, !motionDisabled)
                capture("search-next-phase")
                if (motionDisabled) {
                    fun setMotionScale(value: String) {
                        android.os.ParcelFileDescriptor
                            .AutoCloseInputStream(
                                instrumentation.uiAutomation.executeShellCommand("settings put global animator_duration_scale $value"),
                            ).use { it.readBytes() }
                    }
                    try {
                        setMotionScale("1.0")
                        waitUntil { loader!!.isAnimating }
                    } finally {
                        setMotionScale("0")
                    }
                    waitUntil { !loader!!.isAnimating }
                }
                requests.first().complete(
                    "<tags count=\"1\" available=\"2\"><tag><id>2147483017</id><Title>Barber pole fixture</Title><Downloaded>123</Downloaded></tag></tags>",
                )
                waitUntil { requests.size >= 2 && loader!!.loading }
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment
                    assertEquals(1, fragment.model!!.tags.size)
                    assertTrue(fragment.model!!.isLoading)
                }
                Thread.sleep(250)
                capture("search-pagination")
                scenario.moveToState(Lifecycle.State.CREATED)
                assertFalse(loader!!.isAnimating)
                withLaunchMotionStopped { scenario.moveToState(Lifecycle.State.RESUMED) }
                scenario.onActivity {
                    loader = it.findViewById(R.id.loadingProgressBar)
                }
                waitUntil {
                    var ready = false
                    scenario.onActivity { ready = loader!!.isAnimating == !motionDisabled }
                    ready
                }
                scenario.onActivity { activity ->
                    // Prevent ListView's near-end callback from immediately retrying the failed page.
                    (activity.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment).model!!.hasMoreResults =
                        false
                }
                requests[1].complete("<invalid")
                waitUntil { !loader!!.loading }
                assertFalse(loader!!.isAnimating)
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment
                    assertNotNull(fragment.model!!.statusText)
                    fragment.model!!.hasMoreResults = true
                    fragment.refresh()
                }
                waitUntil { requests.size >= 3 && loader!!.loading }
                requests[2].complete()
                waitUntil { !loader!!.loading }
                assertFalse(loader!!.isAnimating)
                assertEquals(View.GONE, loader!!.visibility)
            }
            requests.clear()
            launchPending<TagBrowserActivity>(Intent(context, TagBrowserActivity::class.java)).use { scenario ->
                var first: BarberPoleLoadingView? = null
                waitUntil {
                    scenario.onActivity { activity ->
                        first =
                            activity.supportFragmentManager.fragments
                                .filterIsInstance<TagQueryFragment>()
                                .firstOrNull { it.isResumed }
                                ?.view
                                ?.findViewById(R.id.loadingProgressBar)
                    }
                    first?.loading == true
                }
                Thread.sleep(300)
                capture("browse-pending")
                scenario.onActivity { activity ->
                    val tabs = activity.findViewById<View>(R.id.tabLayout)
                    val a = IntArray(2)
                    val b = IntArray(2)
                    first!!.getLocationOnScreen(a)
                    tabs.getLocationOnScreen(b)
                    assertTrue("Pole above full-width tabs", a[1] + first!!.height <= b[1])
                    activity.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(1, false)
                }
                waitUntil { !first!!.hostResumed }
                assertFalse(first!!.isAnimating)
                scenario.onActivity { activity ->
                    val visible =
                        activity.supportFragmentManager.fragments
                            .filterIsInstance<TagQueryFragment>()
                            .single { it.isResumed }
                    assertTrue(visible.view!!.findViewById<BarberPoleLoadingView>(R.id.loadingProgressBar).loading)
                }
            }
        } finally {
            requests.forEach { it.complete() }
        }
    }

    private fun setMotionScale(value: String) {
        android.os.ParcelFileDescriptor
            .AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand("settings put global animator_duration_scale $value"),
            ).use { it.readBytes() }
    }

    private inline fun <T> withLaunchMotionStopped(block: () -> T): T {
        // Instrumentation's synchronous first-draw barrier must not wait on an infinite loop.
        // Restore motion immediately after launch; all movement assertions run at the original scale.
        val original =
            android.provider.Settings.Global.getString(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            )
        setMotionScale("0")
        return try {
            block()
        } finally {
            setMotionScale(original ?: "1.0")
        }
    }

    private inline fun <reified T : android.app.Activity> launchPending(intent: Intent): ActivityScenario<T> =
        withLaunchMotionStopped { ActivityScenario.launch<T>(intent) }

    private fun draw(pole: BarberPoleLoadingView): Bitmap {
        lateinit var bitmap: Bitmap
        instrumentation.runOnMainSync {
            bitmap = Bitmap.createBitmap(pole.width, pole.height, Bitmap.Config.ARGB_8888)
            pole.draw(Canvas(bitmap))
        }
        return bitmap
    }

    private fun assertFrames(
        first: Bitmap,
        second: Bitmap,
        moving: Boolean,
    ) {
        var changes = 0
        for (y in 0 until first.height) {
            for (x in 0 until first.width) {
                val same = first.getPixel(x, y) == second.getPixel(x, y)
                if (y < first.height * 10 / 52 || y >= first.height * 42 / 52) assertTrue("Caps stationary", same)
                if (!same) changes++
            }
        }
        if (moving) assertTrue("Interior stripes move", changes > 100) else assertEquals("Remove Animations is static", 0, changes)
        first.recycle()
        second.recycle()
    }

    private fun waitUntil(check: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 8000

        fun onMain(): Boolean {
            var ready = false
            instrumentation.runOnMainSync { ready = check() }
            return ready
        }
        while (!onMain() && SystemClock.uptimeMillis() < deadline) Thread.sleep(50)
        assertTrue("Pending query state reached", onMain())
    }

    private fun capture(name: String) {
        val label = InstrumentationRegistry.getArguments().getString("captureLabel") ?: return
        lateinit var pole: BarberPoleLoadingView
        var wasResumed = false
        lateinit var observer: android.view.ViewTreeObserver.OnPreDrawListener
        // Do not use Espresso/idle waits while an intentionally infinite animation is running.
        waitUntil {
            val activity =
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                    .getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .single()
            pole = activity.findViewById(R.id.loadingProgressBar)
            pole.getGlobalVisibleRect(android.graphics.Rect())
        }
        instrumentation.runOnMainSync {
            assertTrue("Capture has an actual pending query", pole.loading)
            assertTrue("Capture pole is in bounds", pole.getGlobalVisibleRect(android.graphics.Rect()))
            wasResumed = pole.hostResumed
            // Freeze only the drawing phase during native capture. QueryModel and its request stay pending.
            // Natural moving-frame pixels are tested separately above; no production test API is added.
            pole.hostResumed = false
            observer =
                BarberPoleLoadingView::class.java
                    .getDeclaredField("layoutObserver")
                    .apply { isAccessible = true }
                    .get(pole) as android.view.ViewTreeObserver.OnPreDrawListener
            pole.viewTreeObserver.removeOnPreDrawListener(observer)
            BarberPoleLoadingView::class.java
                .getDeclaredField("phase")
                .apply { isAccessible = true }
                .setFloat(pole, if (!motionDisabled && name == "search-next-phase") 0.25f else 0f)
            pole.invalidate()
        }
        try {
            instrumentation.waitForIdleSync()
            Thread.sleep(100)
            val bitmap =
                android.os.ParcelFileDescriptor
                    .AutoCloseInputStream(
                        instrumentation.uiAutomation.executeShellCommand("screencap -p"),
                    ).use { android.graphics.BitmapFactory.decodeStream(it) }
            val scale = 800f / maxOf(bitmap.width, bitmap.height)
            val small = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            File(context.getExternalFilesDir(null), "tagmaster-android-barberpole-$label-$name.jpg").outputStream().use {
                small.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
            small.recycle()
            bitmap.recycle()
        } finally {
            instrumentation.runOnMainSync {
                pole.viewTreeObserver.addOnPreDrawListener(observer)
                pole.hostResumed = wasResumed
            }
        }
    }
}
