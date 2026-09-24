package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performSemanticsAction
import bolts.Task
import bolts.TaskCompletionSource
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Work started from the Summary page and the sheet-music screen (submitting a rating, fetching and
 * decoding sheet music) disables its control while it runs and always gives it back, whether the
 * work succeeds or fails.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SummaryLoadingStateTest : ComposeScreenTest() {
    private fun summary(): Pair<Tag, TaskCompletionSource<Boolean>> {
        val tag =
            spy(
                Tag().apply {
                    id = 2147483089
                    title = "Loading fixture"
                    parts = 4
                    sheetMusicUri =
                        RemoteLocation().apply {
                            uri = "https://example.invalid/compact.pdf"
                            type = "pdf"
                        }
                },
            )
        val rating = TaskCompletionSource<Boolean>()
        doReturn(rating.task).`when`(tag).rate(4)
        launch(TagDetailActivity::class.java, Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, tag.id)) {
            it.tagLoader = { _, _ -> Task.forResult(tag) }
        }
        return tag to rating
    }

    private fun rate() {
        click("rateButton")
        node("ratingPicker").performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
        idle()
        click("ratingSubmit")
    }

    private fun exercise(fail: Boolean) {
        val (_, rating) = summary()
        rate()
        node("rateButton").assertIsNotEnabled()
        if (fail) rating.setError(IllegalStateException("Controlled failure")) else rating.setResult(true)
        idle()
        // A failed rating can be tried again; a submitted one cannot be sent twice.
        if (fail) node("rateButton").assertIsEnabled() else node("rateButton").assertIsNotEnabled()

        val sheet = TaskCompletionSource<File>()
        mockConstruction(ContentCache::class.java) { cache, _ ->
            `when`(cache.loadContentPublic(anyString(), anyString(), anyBoolean())).thenReturn(sheet.task)
        }.use {
            click("sheetMusicLink")
            node("sheetMusicLink").assertIsNotEnabled()
            if (fail) sheet.setError(IllegalStateException("Controlled failure")) else sheet.setResult(File.createTempFile("sheet", ".pdf"))
            idle()
            node("sheetMusicLink").assertIsEnabled()
        }
    }

    @Test
    fun ratingAndSheetCompletionGiveTheControlsBack() = exercise(fail = false)

    @Test
    fun ratingAndSheetFailureGiveTheControlsBack() = exercise(fail = true)

    private fun sheetMusic(valid: Boolean) {
        val output = ByteArrayOutputStream()
        if (valid) {
            Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
                compress(Bitmap.CompressFormat.PNG, 100, output)
                recycle()
            }
        } else {
            output.write("invalid image".toByteArray())
        }
        val uri = Uri.parse("content://compact.fixture/sheet")
        shadowOf(app.contentResolver).registerInputStream(uri, ByteArrayInputStream(output.toByteArray()))
        val activity = launch(SheetMusicActivity::class.java, Intent(app, SheetMusicActivity::class.java).setDataAndType(uri, "image/png"))
        ScreenTestSupport.await("the sheet to decode") { !activity.imageLoading }
        idle()
        assertFalse(activity.imageLoading)
        assertEquals(valid, activity.image != null)
    }

    @Test
    fun theSheetLoadingStateClearsWhenTheImageDecodes() = sheetMusic(valid = true)

    @Test
    fun theSheetLoadingStateClearsWhenTheImageCannotBeDecoded() = sheetMusic(valid = false)
}
