package depollsoft.tagmaster

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import depollsoft.lib.json.JsonSerializer
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real host requests held only at the process-local network boundary. No production delays. */
@RunWith(AndroidJUnit4::class)
class CompactLoadingRegressionTest {
    companion object {
        private class Request(
            val url: URL,
        ) {
            val ready = CountDownLatch(1)
            var body = ""
            var failed = false

            fun finish(
                body: String = "",
                failed: Boolean = false,
            ) {
                this.body = body
                this.failed = failed
                ready.countDown()
            }
        }

        private val requests = CopyOnWriteArrayList<Request>()

        @BeforeClass @JvmStatic
        fun holdRequests() {
            URL.setURLStreamHandlerFactory { protocol ->
                if (protocol != "https") {
                    null
                } else {
                    object : URLStreamHandler() {
                        override fun openConnection(url: URL): URLConnection =
                            object : URLConnection(url) {
                                override fun connect() = Unit

                                override fun getInputStream(): java.io.InputStream {
                                    if (url.host !in
                                        listOf("www.barbershoptags.com", "compact.example.invalid")
                                    ) {
                                        throw IOException("Fixture blocks unrelated network")
                                    }
                                    val request = Request(url).also { requests.add(it) }
                                    check(request.ready.await(40, TimeUnit.SECONDS)) { "Held request timed out" }
                                    if (request.failed) throw IOException("Controlled offline response")
                                    return ByteArrayInputStream(request.body.toByteArray())
                                }
                            }
                    }
                }
            }
        }
    }

    private val instrument get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrument.targetContext
    private val label get() = InstrumentationRegistry.getArguments().getString("captureLabel", "light")

    private fun onMain(block: () -> Unit) = instrument.runOnMainSync(block)

    private fun waitFor(block: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 6000
        while (SystemClock.uptimeMillis() < deadline) {
            var done = false
            onMain { done = block() }
            if (done) return
            Thread.sleep(30)
        }
        var done = false
        onMain { done = block() }
        assertTrue("Timed out waiting for host binding; requests=${requests.map { it.url }}", done)
    }

    private fun nextRequest(after: Int): Request {
        waitFor { requests.size > after }
        return requests[after]
    }

    private fun capture(name: String) {
        // State bindings precede the next hardware-rendered frame. Only the test waits.
        Thread.sleep(120)
        val bitmap = instrument.uiAutomation.takeScreenshot()
        val scale = 800f / maxOf(bitmap.width, bitmap.height)
        val small = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        File(context.getExternalFilesDir(null), "tagmaster-consistent-loading-android-$label-$name.jpg").outputStream().use {
            small.compress(Bitmap.CompressFormat.JPEG, 86, it)
        }
        small.recycle()
        bitmap.recycle()
    }

    private fun checkPending(pole: BarberPoleLoadingView) {
        waitFor { pole.loading && pole.isAnimating }
        onMain {
            assertTrue(pole.hostResumed)
            assertTrue(pole.getGlobalVisibleRect(android.graphics.Rect()))
            assertEquals(context.resources.getDimensionPixelSize(R.dimen.barberpole_compact_height), pole.height)
            assertNotNull(pole.contentDescription)
        }
    }

    private fun checkFinished(pole: BarberPoleLoadingView) {
        waitFor { !pole.loading }
        onMain {
            assertFalse(pole.isAnimating)
            assertEquals(View.INVISIBLE, pole.visibility)
        }
    }

    @Test fun compact_hosts_follow_pending_terminal_and_lifecycle_state() {
        try {
            for (id in listOf(2147483092, 2147483093)) File(context.filesDir, "TagCache/$id").delete()
            ActivityScenario.launch(MeActivity::class.java).use { scenario ->
                var pole: BarberPoleLoadingView? = null
                var count = requests.size
                scenario.onActivity { activity ->
                    pole = activity.findViewById(R.id.randomTagProgress)
                    activity.findViewById<View>(R.id.randomTagButton).performClick()
                }
                var request = nextRequest(count)
                checkPending(pole!!)
                capture("random-row")
                scenario.moveToState(Lifecycle.State.CREATED)
                waitFor { !pole!!.isAnimating }
                // ActivityScenario's first-draw barrier must not wait for an infinite animator.
                val resolver = context.contentResolver
                val originalMotion =
                    android.provider.Settings.Global.getString(
                        resolver,
                        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                    )

                fun motion(value: String) {
                    android.os.ParcelFileDescriptor
                        .AutoCloseInputStream(
                            instrument.uiAutomation.executeShellCommand("settings put global animator_duration_scale $value"),
                        ).use {
                            it.readBytes()
                        }
                }
                motion("0")
                try {
                    scenario.moveToState(Lifecycle.State.RESUMED)
                } finally {
                    motion(originalMotion ?: "1.0")
                }
                checkPending(pole!!)
                onMain { (pole!!.parent as View).visibility = View.INVISIBLE }
                waitFor { !pole!!.isAnimating }
                onMain { (pole!!.parent as View).visibility = View.VISIBLE }
                checkPending(pole!!)
                request.finish(failed = true)
                checkFinished(pole!!)
                count = requests.size
                scenario.onActivity { it.findViewById<View>(R.id.randomTagButton).performClick() }
                request = nextRequest(count)
                checkPending(pole!!)
                request.finish("<tags count=\"0\" available=\"0\"/>")
                checkFinished(pole!!)
            }
            ActivityScenario.launch(TagSearchActivity::class.java).use { scenario ->
                lateinit var row: FavoriteTagItemView
                lateinit var pole: BarberPoleLoadingView
                var count = requests.size
                scenario.onActivity { activity ->
                    val content =
                        LinearLayout(activity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(24, 100, 24, 0)
                        }
                    activity.setContentView(content)
                    row = FavoriteTagItemView(activity)
                    content.addView(row)
                    row.bind(2147483092)
                    pole = row.findViewById(R.id.loadingBar)
                }
                var request = nextRequest(count)
                checkPending(pole)
                capture("saved-row")
                request.finish(failed = true)
                checkFinished(pole)
                count = requests.size
                onMain { row.bind(2147483093) }
                request = nextRequest(count)
                checkPending(pole)
                request.finish(
                    "<tags count=\"1\" available=\"1\"><tag><id>2147483093</id><Title>Compact saved row</Title><Downloaded>12</Downloaded></tag></tags>",
                )
                checkFinished(pole)
                onMain {
                    assertEquals("Compact saved row", row.tag!!.title)
                    row.bind(null)
                }
                checkFinished(pole)
                // Detached/reused rows must not retain a pending loop.
                onMain { (row.parent as android.view.ViewGroup).removeView(row) }
                assertFalse(pole.isAnimating)
                comparison(pole)
            }
            val tag =
                Tag().apply {
                    id = 2147483091
                    title = "Lost"
                    writtenKey = "C"
                    parts = 4
                    rating = 4.5
                    sheetMusicUri =
                        RemoteLocation().apply {
                            uri = "https://compact.example.invalid/${System.nanoTime()}.pdf"
                            type = "pdf"
                        }
                    tenorTrackUri =
                        RemoteLocation().apply {
                            uri = "https://compact.example.invalid/${System.nanoTime()}.mp3"
                            type = "mp3"
                        }
                }
            val cache = File(context.filesDir, "TagCache/${tag.id}")
            cache.parentFile!!.mkdirs()
            cache.writeText(JsonSerializer.serialize(tag).toString())
            ActivityScenario
                .launch<TagDetailActivity>(
                    Intent(context, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, tag.id),
                ).use { scenario ->
                    lateinit var summary: TagSummaryFragment
                    lateinit var rating: BarberPoleLoadingView
                    lateinit var sheet: BarberPoleLoadingView
                    waitFor {
                        var ready = false
                        scenario.onActivity { activity ->
                            summary =
                                activity.supportFragmentManager.fragments
                                    .filterIsInstance<TagSummaryFragment>()
                                    .firstOrNull()
                                    ?: return@onActivity
                            ready = activity.tag != null && summary.view != null
                            if (ready) {
                                rating = summary.requireView().findViewById(R.id.ratingSubmitProgress)
                                sheet =
                                    summary.requireView().findViewById(R.id.sheetMusicProgress)
                            }
                        }
                        ready
                    }
                    var count = requests.size
                    scenario.onActivity {
                        TagSummaryFragment::class.java
                            .getDeclaredMethod("submitRating", View::class.java, Int::class.javaPrimitiveType)
                            .apply {
                                isAccessible =
                                    true
                            }.invoke(summary, summary.requireView(), 4)
                    }
                    var request = nextRequest(count)
                    checkPending(rating)
                    capture("rating")
                    request.finish(failed = true)
                    checkFinished(rating)
                    count = requests.size
                    scenario.onActivity { summary.requireView().findViewById<View>(R.id.sheetMusicLink).performClick() }
                    request = nextRequest(count)
                    checkPending(sheet)
                    capture("sheet-button")
                    request.finish(failed = true)
                    checkFinished(sheet)
                    scenario.onActivity { activity ->
                        activity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager).setCurrentItem(2, false)
                    }
                    lateinit var player: MediaPlayerView
                    waitFor {
                        var found = false
                        scenario.onActivity { a ->
                            a.findViewById<MediaPlayerView>(R.id.mediaPlayer)?.let {
                                player = it
                                found =
                                    true
                            }
                        }
                        found
                    }
                    count = requests.size
                    scenario.onActivity { activity ->
                        activity.findViewById<View>(R.id.tenorButton).performClick()
                        player.findViewById<View>(R.id.playPauseButton).performClick()
                    }
                    request = nextRequest(count)
                    val track = player.findViewById<BarberPoleLoadingView>(R.id.trackLoadingIndicator)
                    checkPending(track)
                    onMain { assertTrue(player.findViewById<View>(R.id.stopButton).isEnabled) }
                    capture("track-row")
                    request.finish(failed = true)
                    checkFinished(track)
                    count = requests.size
                    scenario.onActivity { activity ->
                        activity.findViewById<View>(R.id.tenorButton).performClick()
                        player.findViewById<View>(R.id.playPauseButton).performClick()
                    }
                    request = nextRequest(count)
                    checkPending(track)
                    scenario.onActivity { player.findViewById<View>(R.id.stopButton).performClick() }
                    checkFinished(track)
                    request.finish("not audio")
                    onMain { assertFalse(player.isPlaying) }
                }
        } finally {
            requests.forEach { it.finish(failed = true) }
        }
    }

    private fun comparison(compact: BarberPoleLoadingView) {
        onMain {
            val regular = BarberPoleLoadingView(context)
            val image = Bitmap.createBitmap(500, 230, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(image)
            val dark = label == "dark"
            canvas.drawColor(if (dark) 0xff252525.toInt() else 0xfffafafa.toInt())
            val text =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (dark) -1 else 0xff222222.toInt()
                    textSize = 18f
                }
            canvas.drawText("Android • same artwork, phase 0.25", 16f, 25f, text)
            for ((index, pole) in listOf(compact, regular).withIndex()) {
                val h = if (index == 0) 32 else 58
                val w = if (index == 0) 19 else 34
                pole.layout(0, 0, w, h)
                val art = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                pole.drawPole(Canvas(art), .25f)
                canvas.drawBitmap(Bitmap.createScaledBitmap(art, w * 2, h * 2, true), 70f + index * 230, 50f, null)
                canvas.drawText(if (index == 0) "Compact 19×32" else "Search 34×58", 25f + index * 230, 205f, text)
            }
            File(context.getExternalFilesDir(null), "tagmaster-consistent-loading-android-$label-comparison.jpg").outputStream().use {
                image.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
            image.recycle()
        }
    }
}
