package depollsoft.pitchperfect

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
        // Capture the real ad-free state; never show a debug test advertisement.
        PurchaseService.areAdsRemoved = true
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val list = SongsModel.get().defaultSongList
            list.resetSongs()
            listOf("Blue Skies", "Down Our Way", "Heart of My Heart", "Shenandoah", "Sweet Adeline", "The Old Songs", "When You Were Sweet Sixteen", "You Are My Sunshine").forEachIndexed { index, title ->
                list.addSong(depollsoft.pitchperfect.lib.PitchedSong().apply {
                    name = title
                    key = depollsoft.pitchperfect.lib.Key.getMajorKeys()[index + 2]
                })
            }
        }
        ActivityScenario.launch(PitchPerfectActivity::class.java).use { scenario ->
            for ((tab, name) in listOf(R.id.pitchpipe_item to "01-pitch-pipe", R.id.notes_item to "02-notes", R.id.keys_item to "03-keys", R.id.songs_item to "04-songs")) {
                scenario.onActivity { activity ->
                    PurchaseService.areAdsRemoved = true
                    activity.findViewById<android.view.View>(tab).performClick()
                    check(!activity.adsShouldShow)
                }
                capture(name)
            }
            scenario.onActivity { activity ->
                activity.supportFragmentManager.fragments.filterIsInstance<SongListFragment>().single().toggleEditingSongs()
            }
            capture("05-edit-songs")
            val intent = android.content.Intent(instrumentation.targetContext, AddSongActivity::class.java)
                .putExtra(AddSongActivity.ID_EXTRA, SongsModel.get().defaultSongList.songs[0].id)
            ActivityScenario.launch<AddSongActivity>(intent).use {
                capture("06-song-editor")
            }
        }
    }
}
