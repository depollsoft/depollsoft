package depollsoft.tagmaster

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Scoped-storage cleanup for the optional quartet frames TagLoadingRegressionTest writes, never
 * user material. Run with `spacingCaptures=true` to copy them to the cache before deleting them,
 * `removeQuartetFrames=true` to delete them, and `expectEmptyExternal=true` to check nothing else
 * is left.
 */
@RunWith(AndroidJUnit4::class)
class LayoutCaptureCleanupTest {
    @Test fun exportAndRemoveOnlyGeneratedFrames() {
        val arguments = InstrumentationRegistry.getArguments()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val external = context.getExternalFilesDir(null)!!
        val frames = File(external, "quartet-frames")
        if (arguments.getString("spacingCaptures") == "true" && frames.exists()) {
            val spacing = File(context.cacheDir, "spacing-captures")
            frames.walkTopDown().filter { it.isFile }.forEach { frame ->
                val copy = File(spacing, "quartet-frames/${frame.relativeTo(frames)}")
                copy.parentFile!!.mkdirs()
                frame.copyTo(copy, overwrite = true)
                assertArrayEquals(frame.readBytes(), copy.readBytes())
            }
            assertTrue(frames.deleteRecursively())
        }
        if (arguments.getString("removeQuartetFrames") == "true" && frames.exists()) {
            assertTrue(frames.deleteRecursively())
        }
        if (arguments.getString("expectEmptyExternal") == "true") {
            assertTrue("Original external files directory was empty", external.listFiles()!!.isEmpty())
        }
    }
}
