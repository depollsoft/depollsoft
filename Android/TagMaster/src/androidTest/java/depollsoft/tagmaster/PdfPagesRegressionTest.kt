package depollsoft.tagmaster

import android.content.Intent
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import depollsoft.tagmaster.barbershop.Tag
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Uses Android's real PDF renderer, not a mocked page-index loop. */
@RunWith(AndroidJUnit4::class)
class PdfPagesRegressionTest {
    @Test fun renders_each_pdf_page_once_in_order() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(context.cacheDir, "regression-two-pages.pdf")
        val pdf = PdfDocument()
        try {
            for ((index, color) in listOf(Color.RED, Color.BLUE).withIndex()) {
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(72, 72, index + 1).create())
                page.canvas.drawColor(color)
                pdf.finishPage(page)
            }
            file.outputStream().use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
        // Avoid any catalog/network dependency in this rendering test.
        Tag()
            .apply {
                id = 2147483000
                title = "PDF regression"
            }.cache()
        val intent =
            Intent(context, SheetMusicActivity::class.java)
                .setDataAndType(Uri.fromFile(file), "application/pdf")
                .putExtra("tagId", 2147483000)
        try {
            ActivityScenario.launch<SheetMusicActivity>(intent).use { scenario ->
                var rendered = false
                val deadline = System.currentTimeMillis() + 10000
                while (!rendered && System.currentTimeMillis() < deadline) {
                    scenario.onActivity { activity ->
                        val bitmap = activity.image
                        if (bitmap != null) {
                            assertEquals(bitmap.width * 2, bitmap.height)
                            assertEquals(Color.RED, bitmap.getPixel(bitmap.width / 2, bitmap.height / 4))
                            assertEquals(Color.BLUE, bitmap.getPixel(bitmap.width / 2, bitmap.height * 3 / 4))
                            assertFalse(activity.imageLoading)
                            rendered = true
                        }
                    }
                    if (!rendered) Thread.sleep(50)
                }
                assertTrue("Two distinct pages must render before the deadline", rendered)
            }
        } finally {
            file.delete()
        }
    }
}
