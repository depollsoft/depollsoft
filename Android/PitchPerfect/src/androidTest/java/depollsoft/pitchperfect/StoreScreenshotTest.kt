package depollsoft.pitchperfect

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

    @Test fun captureStoreScreenshots() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("storeScreenshots") == "true")
        // Capture the real ad-free state; never show a debug test advertisement.
        PurchaseService.areAdsRemoved = true
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Capture a returning user's library with optional telemetry declined.
        depollsoft.lib.privacy.PrivacyChoices(instrumentation.targetContext)
            .save(analytics = false, crashes = false)
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
            for ((tab, name) in listOf(MainTab.PITCH_PIPE to "01-pitch-pipe", MainTab.NOTES to "02-notes", MainTab.KEYS to "03-keys", MainTab.SONGS to "04-songs")) {
                scenario.onActivity { activity ->
                    PurchaseService.areAdsRemoved = true
                    activity.showTab(tab)
                    check(!activity.adsShouldShow)
                }
                capture(name)
            }
            scenario.onActivity { activity ->
                activity.songs.toggleEditing()
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
