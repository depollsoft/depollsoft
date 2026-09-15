package depollsoft.tagmaster

import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StoreScreenshotTest {
    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        Thread.sleep(1000)
        val image = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = File(instrumentation.targetContext.filesDir, "store-screenshots")
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    @Test fun captureStoreScreenshots() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("storeScreenshots") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = android.content.Intent(context, TagDetailActivity::class.java)
            .putExtra(TagDetailActivity.TAG_ID_EXTRA, 1809)
        ActivityScenario.launch<TagDetailActivity>(intent).use { scenario ->
            lateinit var activity: TagDetailActivity
            scenario.onActivity { activity = it }
            fun onActivity(action: (TagDetailActivity) -> Unit) {
                // Loading animations continuously invalidate the UI. Poll on the
                // main thread without waiting for global Espresso/Looper idleness.
                InstrumentationRegistry.getInstrumentation().runOnMainSync { action(activity) }
            }
            val deadline = System.currentTimeMillis() + 60000
            var loaded = false
            while (!loaded && System.currentTimeMillis() < deadline) {
                onActivity { loaded = it.tag != null && !it.isLoading }
                Thread.sleep(200)
            }
            check(loaded) { "Live tag 1809 did not load; refusing empty store screenshots" }
            for ((page, name) in listOf(0 to "01-summary", 1 to "02-details", 2 to "03-tracks")) {
                onActivity {
                    it.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager).setCurrentItem(page, false)
                }
                if (page == 2) {
                    onActivity {
                        it.findViewById<android.view.View>(R.id.leadButton).performClick()
                        it.findViewById<android.view.View>(R.id.playPauseButton).performClick()
                    }
                    val trackDeadline = System.currentTimeMillis() + 60000
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
                capture(name)
            }
        }
    }
}
