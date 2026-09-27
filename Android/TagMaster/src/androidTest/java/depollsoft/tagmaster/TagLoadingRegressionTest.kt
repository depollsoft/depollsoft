package depollsoft.tagmaster

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorSpace
import androidx.test.ext.junit.runners.AndroidJUnit4
import depollsoft.tagmaster.ui.QuartetRenderer
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Opt-in native capture: the Android half of the cross-platform quartet frame comparison.
 *
 * This is capture only. Everything the instrumented original asserted about the detail screen's
 * loading state machine — pending and loaded visibility, menu enablement, failure, retry, failed
 * refresh, the motion gate and late completions belonging to a closed screen — now runs on the JVM
 * in `TagLoadingStateTest`, so nothing here is a regression guard. What cannot move to the JVM is
 * the frame export itself: `iOS/tagmaster/tools/compare_quartet_frames.py` compares these PNGs
 * against the iOS renderer's, and that comparison is only meaningful against frames a real device
 * rasterised.
 *
 * It is gated the same way the other capture residue in this source set is, so an ordinary
 * instrumented run skips it:
 *
 * ```
 * ./gradlew :TagMaster:connectedDebugAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.quartetFrames=true \
 *     -Pandroid.testInstrumentationRunnerArguments.class=\
 * depollsoft.tagmaster.TagLoadingRegressionTest#controlled_pending_to_loaded_capture
 * ```
 */
@androidx.test.filters.SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class TagLoadingRegressionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as Application

    @Test fun controlled_pending_to_loaded_capture() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("quartetFrames") == "true")
        instrumentation.runOnMainSync {
            val directory = File(app.getExternalFilesDir(null), "quartet-frames").apply { mkdirs() }
            for (dark in listOf(false, true)) {
                for (scale in listOf(1, 10)) {
                    val width = 216 * scale
                    val height = 96 * scale
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
                                width,
                                height,
                                Bitmap.Config.ARGB_8888,
                                true,
                                ColorSpace.get(ColorSpace.Named.SRGB),
                            )
                        QuartetRenderer.draw(Canvas(bitmap), width, height, phase, moving = label != "still", dark = dark)
                        File(directory, "android-${if (dark) "dark" else "light"}-$scale-$label.png")
                            .outputStream()
                            .use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        bitmap.recycle()
                    }
                }
            }
        }
    }
}
