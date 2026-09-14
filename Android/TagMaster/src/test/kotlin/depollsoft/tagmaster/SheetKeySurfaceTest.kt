package depollsoft.tagmaster

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [28])
class SheetKeySurfaceTest {
    @Test fun light_floating_key_has_an_opaque_surface_in_every_state() = verifySurface()

    @Test
    @Config(qualifiers = "night")
    fun dark_floating_key_has_an_opaque_surface_in_every_state() = verifySurface()

    private fun verifySurface() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup()
        try {
            val activity = controller.get()
            activity.setContentView(R.layout.sheetmusicview)
            val key = activity.findViewById<ExtendedFloatingActionButton>(R.id.keyButton)
            key.text = "Major:Eb"
            key.measure(
                View.MeasureSpec.makeMeasureSpec(420, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(168, View.MeasureSpec.EXACTLY),
            )
            key.layout(0, 0, 420, 168)
            for ((enabled, playing) in listOf(true to false, true to true, true to false, false to true, false to false)) {
                key.isEnabled = enabled
                key.isActivated = playing
                key.background.jumpToCurrentState()
                val fill = key.backgroundTintList!!.getColorForState(key.drawableState, Color.TRANSPARENT)
                assertEquals("An elevated sheet key must not have a transparent body", 255, Color.alpha(fill))
                if (enabled) {
                    assertTrue("Button text stays readable", ColorUtils.calculateContrast(key.currentTextColor, fill) >= 4.5)
                    assertEquals(key.currentTextColor, key.iconTint!!.getColorForState(key.drawableState, 0))
                }

                fun render(background: Int): Bitmap =
                    Bitmap.createBitmap(key.width, key.height, Bitmap.Config.ARGB_8888).also {
                        it.eraseColor(background)
                        key.background.setBounds(0, 0, key.width, key.height)
                        key.background.draw(Canvas(it))
                    }
                val overPaper = render(Color.WHITE)
                val overInk = render(Color.BLACK)
                // Check a wide interior region, not a single pixel that could land on the border.
                for (y in key.height / 3 until key.height * 2 / 3) {
                    for (x in key.width / 3 until key.width * 2 / 3) {
                        assertEquals("Sheet music cannot bleed through the button", overPaper.getPixel(x, y), overInk.getPixel(x, y))
                    }
                }
                overPaper.recycle()
                overInk.recycle()
                assertEquals(420, key.width)
                assertEquals(168, key.height)
            }
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
