package depollsoft.tagmaster

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.SystemClock
import android.system.Os
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import depollsoft.lib.json.JsonSerializer
import depollsoft.tagmaster.barbershop.Tag
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SheetMusicPdfTest {
    @Test
    fun twoPagePdfRendersDistinctPagesInOrderAndClosesFile() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val pdf = File.createTempFile("chart-two-pages-", ".pdf", context.cacheDir)
        val tagFile = File(context.filesDir, "TagCache/2147483646")
        check(!tagFile.exists()) { "Fixture must not overwrite a cached tag" }
        try {
            val document = PdfDocument()
            try {
                listOf(Color.RED, Color.BLUE).forEachIndexed { pageIndex, color ->
                    val page = document.startPage(PdfDocument.PageInfo.Builder(144, 144, pageIndex + 1).create())
                    page.canvas.drawColor(color)
                    page.canvas.drawText(
                        "Page ${pageIndex + 1}",
                        12f,
                        32f,
                        Paint().apply {
                            this.color = Color.WHITE
                            textSize = 20f
                        },
                    )
                    document.finishPage(page)
                }
                pdf.outputStream().use { document.writeTo(it) }
            } finally {
                document.close()
            }
            // Seed the normal disk cache so opening the actual activity needs no catalog request.
            tagFile.parentFile!!.mkdirs()
            tagFile.writeText(
                JsonSerializer
                    .serialize(
                        Tag().apply {
                            id = 2147483646
                            title = "Two-page chart regression"
                        },
                    ).toString(),
            )
            val intent =
                Intent(context, SheetMusicActivity::class.java)
                    .setDataAndType(Uri.fromFile(pdf), "application/pdf")
                    .putExtra("tagId", 2147483646)
            ActivityScenario.launch<SheetMusicActivity>(intent).use { scenario ->
                var rendered: Bitmap? = null
                val deadline = SystemClock.uptimeMillis() + 10000
                while (rendered == null && SystemClock.uptimeMillis() < deadline) {
                    scenario.onActivity {
                        rendered = (it.photoView.drawable as? BitmapDrawable)?.bitmap
                    }
                    if (rendered == null) SystemClock.sleep(50)
                }
                assertNotNull("The real PDF viewer must display the generated chart", rendered)
                val bitmap = rendered!!
                assertEquals("Two square pages must be stacked vertically", bitmap.width * 2, bitmap.height)
                val first = bitmap.getPixel(bitmap.width / 2, bitmap.height / 4)
                val second = bitmap.getPixel(bitmap.width / 2, bitmap.height * 3 / 4)
                assertEquals("Page 1 must stay first", Color.RED, first)
                assertEquals("Page 2 must be blue, not a duplicate of page 1", Color.BLUE, second)
                assertNotEquals(first, second)
                val openPdfDescriptors =
                    File("/proc/self/fd").listFiles()!!.filter {
                        try {
                            Os.readlink(it.path) == pdf.canonicalPath
                        } catch (_: Exception) {
                            false
                        }
                    }
                assertTrue("PdfRenderer must close its owned file descriptor", openPdfDescriptors.isEmpty())
                android.util.Log.i("SheetMusicPdfTest", "PASS: ${bitmap.width}x${bitmap.height}, first=red, second=blue, PDF descriptors=0")
                instrumentation.waitForIdleSync()
                val screenshot = instrumentation.uiAutomation.takeScreenshot()
                val scale = minOf(1f, 800f / maxOf(screenshot.width, screenshot.height))
                val preview =
                    Bitmap.createScaledBitmap(
                        screenshot,
                        (screenshot.width * scale).toInt(),
                        (screenshot.height * scale).toInt(),
                        true,
                    )
                File(context.cacheDir, "chart-two-pages-preview.png").outputStream().use {
                    preview.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                if (preview !== screenshot) preview.recycle()
                screenshot.recycle()
            }
        } finally {
            pdf.delete()
            tagFile.delete()
        }
    }
}
