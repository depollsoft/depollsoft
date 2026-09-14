package depollsoft.tagmaster

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FooterPitchLayoutTest {
    @Test fun copyright_refreshes_when_the_window_returns() =
        withActivity { activity ->
            val footer = LayoutInflater.from(activity).inflate(R.layout.meviewfooter, null) as ViewGroup
            val copyright = footer.findViewById<TextView>(R.id.copyrightTextView)
            val expected = "© ${java.util.GregorianCalendar().get(java.util.Calendar.YEAR)}"
            assertEquals(expected, copyright.text.toString())
            footer.dispatchWindowVisibilityChanged(View.GONE)
            copyright.text = "© 2021"
            footer.dispatchWindowVisibilityChanged(View.VISIBLE)
            assertEquals(expected, copyright.text.toString())
        }

    @Test fun footer_wraps_whole_links_at_phone_and_sidebar_widths() =
        withActivity { activity ->
            for (scale in listOf(1f, 1.3f, 2f)) {
                for (widthDp in listOf(320, 360, 600)) {
                    val config = Configuration(activity.resources.configuration).apply { fontScale = scale }
                    val context = android.view.ContextThemeWrapper(activity.createConfigurationContext(config), R.style.AppTheme)
                    val footer = LayoutInflater.from(context).inflate(R.layout.meviewfooter, null) as ViewGroup
                    val density = context.resources.displayMetrics.density
                    val width = (widthDp * density).toInt()
                    footer.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    )
                    footer.layout(0, 0, width, footer.measuredHeight)
                    if (scale == 1f &&
                        widthDp == 360
                    ) {
                        assertTrue("Default height ${footer.height / density}", footer.height / density <= 144f)
                    }
                    val targets =
                        listOf(R.id.TextView01, R.id.textView4, R.id.termsHyperlink, R.id.donateHyperlink).map { id ->
                            val text = footer.findViewById<TextView>(id)
                            assertTrue(text.height / density >= 48f)
                            assertTrue(text.width / density >= 48f)
                            assertTrue(text.isClickable)
                            assertTrue(
                                (0 until text.lineCount).all {
                                    text.layout.getEllipsisCount(it) == 0 &&
                                        text.layout.getLineWidth(it) <=
                                        text.width - text.compoundPaddingLeft - text.compoundPaddingRight + 1
                                },
                            )
                            Rect(0, 0, text.width, text.height).also { footer.offsetDescendantRectToMyCoords(text, it) }
                        }
                    targets.forEachIndexed { index, rect ->
                        assertTrue(
                            "Bounds at $widthDp/$scale: $rect",
                            rect.left >= 0 && rect.right <= width && rect.bottom <= footer.height,
                        )
                        targets.drop(index + 1).forEach { assertFalse(Rect.intersects(rect, it)) }
                    }
                    if (scale == 1f) assertEquals("Terms and Donate share a row", targets[2].top, targets[3].top)
                    assertEquals(context.getString(R.string.app_name), footer.findViewById<TextView>(R.id.appNameTextView).text.toString())
                    assertEquals(
                        context.getString(R.string.app_version),
                        footer.findViewById<TextView>(R.id.appVersionTextView).text.toString(),
                    )
                    assertEquals(
                        "© ${java.util.GregorianCalendar().get(java.util.Calendar.YEAR)}",
                        footer.findViewById<TextView>(R.id.copyrightTextView).text.toString(),
                    )
                    val urls =
                        mapOf(
                            R.id.TextView01 to "http://apps.depoll.com",
                            R.id.textView4 to "http://www.barbershoptags.com",
                            R.id.termsHyperlink to "http://apps.depoll.com/terms-of-use",
                            R.id.donateHyperlink to "http://www.davidpoll.com/applications/tag-master/donate",
                            R.id.marketHyperlink to "https://market.android.com/details?id=depollsoft.pitchperfect",
                        )
                    urls.forEach { (id, url) -> assertEquals(url, footer.findViewById<View>(id).tag) }
                    assertEquals(View.GONE, footer.findViewById<View>(R.id.marketHyperlink).visibility)
                }
            }
        }

    @Test fun light_pitch_pixels_and_contrast() = checkPitch()

    @Test
    @Config(qualifiers = "night")
    fun dark_pitch_pixels_and_contrast() = checkPitch()

    @Test
    @Config(qualifiers = "land")
    fun landscape_pitch_pixels_and_contrast() = checkPitch()

    private fun checkPitch() =
        withActivity { activity ->
            val root = LayoutInflater.from(activity).inflate(R.layout.tagsummaryview, null)
            val key = root.findViewById<TextView>(R.id.playKeyNoteButton)
            val width = 240
            val height = 120
            key.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            key.layout(0, 0, width, height)
            for ((enabled, pressed) in listOf(true to false, true to true, true to false, false to true, false to false)) {
                key.isEnabled = enabled
                key.isPressed = pressed
                key.isActivated = pressed
                key.background.jumpToCurrentState()
                val playing = enabled && pressed
                val fill = if (playing) activity.getColor(R.color.md_primary) else Color.TRANSPARENT
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                key.background.setBounds(0, 0, width, height)
                key.background.draw(Canvas(bitmap))
                assertEquals("Actual fill enabled=$enabled pressed=$pressed", fill, bitmap.getPixel(width / 2, height / 2))
                if (enabled) {
                    val ink = activity.getColor(if (playing) R.color.md_on_primary else R.color.md_primary)
                    assertEquals(ink, key.currentTextColor)
                    val background = if (playing) fill else activity.getColor(R.color.md_surface)
                    assertTrue(ColorUtils.calculateContrast(ink, background) >= 4.5)
                    key.draw(Canvas(bitmap)) // TextView applies stateful compound drawable tint during drawing.
                    val icon = key.compoundDrawablesRelative[0]
                    // Apply the declared framework tint explicitly in the SDK28 resource probe.
                    // Native tests below cover TextView applying this selector itself.
                    assertNotNull(key.compoundDrawableTintList)
                    icon.setTintList(key.compoundDrawableTintList)
                    icon.state = key.drawableState
                    bitmap.eraseColor(Color.TRANSPARENT)
                    icon.setBounds(0, 0, width, height)
                    icon.draw(Canvas(bitmap))
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                    assertTrue(
                        "Rendered icon ink expected=$ink actual=${pixels.filter { Color.alpha(it) > 128 }.distinct().take(8)}",
                        pixels.any { Color.alpha(it) > 128 && (it and 0xFFFFFF) == (ink and 0xFFFFFF) },
                    )
                } else {
                    assertTrue(Color.alpha(key.currentTextColor) < 128)
                }
                assertEquals(width, key.width)
                assertEquals(height, key.height)
                bitmap.recycle()
            }
        }

    private fun withActivity(block: (Activity) -> Unit) {
        val controller = Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup()
        try {
            block(controller.get())
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
