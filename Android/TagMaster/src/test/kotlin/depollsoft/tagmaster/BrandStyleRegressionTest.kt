package depollsoft.tagmaster

import androidx.appcompat.app.AppCompatActivity
import android.app.Application
import android.graphics.Color
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.view.LayoutInflater
import android.widget.Button
import androidx.core.widget.ImageViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class BrandStyleRegressionTest {
    private fun withActivity(check: (AppCompatActivity) -> Unit) {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup()
        try {
            check(controller.get())
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test fun light_palette_and_actions() = checkPaletteAndActions("#007AA3", "#FFFFFF", "#2E7D32")

    @Test
    @Config(qualifiers = "night")
    fun dark_palette_and_actions() = checkPaletteAndActions("#5AC8FA", "#00344A", "#81C784")

    @Test
    @Config(qualifiers = "land")
    fun landscape_summary_keeps_the_action_hierarchy() = checkPaletteAndActions("#007AA3", "#FFFFFF", "#2E7D32")

    private fun checkPaletteAndActions(
        primaryHex: String,
        onPrimaryHex: String,
        availableHex: String,
    ) = withActivity { activity ->
        val primary = Color.parseColor(primaryHex)
        val onPrimary = Color.parseColor(onPrimaryHex)
        assertEquals(primary, activity.getColor(R.color.md_primary))
        assertEquals(onPrimary, activity.getColor(R.color.md_on_primary))
        assertEquals(primary, activity.getColor(R.color.md_secondary))
        assertEquals(Color.parseColor("#373737"), activity.getColor(R.color.brand_chrome))
        assertEquals(Color.WHITE, activity.getColor(R.color.brand_on_chrome))
        assertEquals(Color.parseColor(availableHex), activity.getColor(R.color.status_available))
        val root = LayoutInflater.from(activity).inflate(R.layout.tagsummaryview, null)
        val sheet = root.findViewById<MaterialButton>(R.id.sheetMusicLink)
        val key = root.findViewById<Button>(R.id.playKeyNoteButton)
        val rate = root.findViewById<android.widget.ImageButton>(R.id.rateButton)
        assertEquals(primary, sheet.backgroundTintList!!.defaultColor)
        assertEquals(onPrimary, sheet.currentTextColor)
        assertNotNull(sheet.icon)
        assertEquals(primary, key.currentTextColor)
        assertNull("Framework tint must not fill the outlined key", key.backgroundTintList)
        assertNotNull(key.compoundDrawablesRelative[0])
        assertEquals(primary, ImageViewCompat.getImageTintList(rate)!!.defaultColor)
        val height = (48 * activity.resources.displayMetrics.density).toInt()
        for (button in listOf(sheet, key, rate)) assertEquals(height, button.layoutParams.height)
        val indicator = StatusIndicatorView(activity).apply { text = "Sheet music" }
        val body = MaterialColors.getColor(indicator, com.google.android.material.R.attr.colorOnSurface)
        for (available in listOf(false, true)) {
            indicator.setIsAvailable(available)
            assertEquals(available, indicator.getIsAvailable())
            assertEquals(body, indicator.currentTextColor)
            val icon = indicator.compoundDrawablesRelative[0]!!
            val bitmap = android.graphics.Bitmap.createBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
            icon.setBounds(0, 0, 48, 48)
            icon.draw(android.graphics.Canvas(bitmap))
            val pixels = IntArray(48 * 48)
            bitmap.getPixels(pixels, 0, 48, 0, 0, 48, 48)
            val tint = if (available) activity.getColor(R.color.status_available) else
                MaterialColors.getColor(indicator, com.google.android.material.R.attr.colorOnSurfaceVariant)
            // Vector edges are antialiased. Compare RGB on a covered pixel, not alpha=255.
            assertTrue("Availability icon uses its semantic tint", pixels.any {
                Color.alpha(it) > 128 && (it and 0xFFFFFF) == (tint and 0xFFFFFF)
            })
            bitmap.recycle()
            assertTrue(indicator.contentDescription.contains("Sheet music"))
        }
    }

    @Test fun chrome_paints_only_the_inherited_status_inset() = withActivity { activity ->
        activity.setContentView(R.layout.meview)
        activity.setUpToolbar(showUp = false)
        val content = activity.findViewById<android.view.View>(android.R.id.content)
        content.setPadding(0, 12, 0, 4)
        val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
        content.background.setBounds(0, 0, 100, 100)
        content.background.draw(android.graphics.Canvas(bitmap))
        assertEquals(activity.getColor(R.color.brand_chrome), bitmap.getPixel(50, 6))
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(50, 50))
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(50, 98))
        bitmap.recycle()
    }

    @Test fun handwriting_measurement_and_drawing_keep_full_bounds_inside_the_toolbar() =
        withActivity { activity ->
            val title = "Tag Master, Lost".makeTitleString(activity) as Spanned
            val spans = title.getSpans(0, title.length, MetricAffectingSpan::class.java)
            val density = activity.resources.displayMetrics.density
            for (scale in listOf(1f, 1.3f, 1.5f, 2f)) {
                val measure = TextPaint().apply { textSize = 22f * density * scale }
                val draw = TextPaint(measure)
                for (span in spans) {
                    span.updateMeasureState(measure)
                    span.updateDrawState(draw)
                }
                assertEquals(measure.textSize, draw.textSize, 0.01f)
                assertTrue("Full font bounds at $scale", measure.fontMetrics.bottom - measure.fontMetrics.top <= 40f * density + 0.1f)
            }
        }
}
