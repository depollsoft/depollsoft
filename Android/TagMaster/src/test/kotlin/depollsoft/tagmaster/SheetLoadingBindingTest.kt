package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import depollsoft.lib.activity.RichApplication
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SheetLoadingBindingTest {
    @Test fun real_sheet_binding_clears_on_decode_success() = render(true)

    @Test fun real_sheet_binding_clears_on_decode_failure_without_external_handler() = render(false)

    private fun render(valid: Boolean) {
        val app = RuntimeEnvironment.getApplication()
        RichApplication::class.java
            .getDeclaredField("context")
            .apply { isAccessible = true }
            .set(null, app)
        Settings.Global.putFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val output = ByteArrayOutputStream()
        if (valid) {
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
        } else {
            output.write("invalid image".toByteArray())
        }
        val entered = CountDownLatch(1)
        val ready = CountDownLatch(1)
        val stream =
            object : ByteArrayInputStream(output.toByteArray()) {
                override fun read(
                    buffer: ByteArray,
                    offset: Int,
                    length: Int,
                ): Int {
                    entered.countDown()
                    check(ready.await(5, TimeUnit.SECONDS))
                    return super.read(buffer, offset, length)
                }
            }
        val uri = Uri.parse("content://compact.fixture/sheet")
        // Robolectric's ContentResolver shadow bypasses ContentProvider.openAssetFile.
        // Hold its registered stream instead; the production decode and binding still run.
        shadowOf(app.contentResolver).registerInputStream(uri, stream)
        val controller =
            Robolectric.buildActivity(
                SheetMusicActivity::class.java,
                Intent(app, SheetMusicActivity::class.java).setDataAndType(uri, "image/png"),
            )
        controller.get().setTheme(R.style.AppTheme)
        try {
            controller.setup().visible()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            shadowOf(Looper.getMainLooper()).idle()
            val pole = controller.get().findViewById<BarberPoleLoadingView>(R.id.sheetMusicLoading)
            assertTrue(pole.loading)
            assertFalse(pole.isAnimating)
            ready.countDown()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (pole.loading && System.nanoTime() < deadline) {
                Thread.sleep(20)
                shadowOf(Looper.getMainLooper()).idle()
            }
            assertFalse(pole.loading)
            assertFalse(pole.isAnimating)
            assertEquals(valid, controller.get().drawable != null)
        } finally {
            ready.countDown()
            controller.pause().stop().destroy()
        }
    }
}
