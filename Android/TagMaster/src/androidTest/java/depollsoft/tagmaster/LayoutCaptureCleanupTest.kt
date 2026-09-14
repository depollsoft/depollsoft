package depollsoft.tagmaster

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Scoped-storage cleanup for this pass's optional native captures, never user material. */
@RunWith(AndroidJUnit4::class)
class LayoutCaptureCleanupTest {
    @Test fun export_and_remove_only_generated_captures() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val external = context.getExternalFilesDir(null)!!
        val export = File(context.cacheDir, "layout-captures").apply { mkdirs() }
        external.listFiles()!!.filter { it.name.startsWith("tagmaster-layout-after-android-") && it.extension == "jpg" }.forEach { image ->
            val copy = File(export, image.name)
            image.copyTo(copy, overwrite = true)
            assertArrayEquals(image.readBytes(), copy.readBytes())
            assertTrue(image.delete())
        }
        if (InstrumentationRegistry.getArguments().getString("spacingCaptures") == "true") {
            val spacing = File(context.cacheDir, "spacing-captures").apply { mkdirs() }
            external.listFiles()!!.filter { it.name.startsWith("tagmaster-android-loading-default-") && it.extension == "jpg" }.forEach { image ->
                val copy = File(spacing, image.name)
                image.copyTo(copy, overwrite = true)
                assertArrayEquals(image.readBytes(), copy.readBytes())
                assertTrue(image.delete())
            }
            val frames = File(external, "quartet-frames")
            if (frames.exists()) {
                frames.walkTopDown().filter { it.isFile }.forEach { frame ->
                    val copy = File(spacing, "quartet-frames/${frame.relativeTo(frames)}")
                    copy.parentFile!!.mkdirs()
                    frame.copyTo(copy, overwrite = true)
                    assertArrayEquals(frame.readBytes(), copy.readBytes())
                }
                assertTrue(frames.deleteRecursively())
            }
        }
        // This existing TagLoadingRegressionTest output is opt-in; remove it only when
        // explicitly requested by the guarded runner after its original-data backup.
        if (InstrumentationRegistry.getArguments().getString("removeQuartetFrames") == "true") {
            val frames = File(external, "quartet-frames")
            if (frames.exists()) assertTrue(frames.deleteRecursively())
        }
        assertFalse(external.listFiles()!!.any { it.name.startsWith("tagmaster-layout-after-android-") })
        if (InstrumentationRegistry.getArguments().getString("expectEmptyExternal") == "true") {
            assertTrue("Original external files directory was empty", external.listFiles()!!.isEmpty())
        }
    }
}
