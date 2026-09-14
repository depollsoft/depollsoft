package depollsoft.tagmaster

import android.app.Application
import android.text.Layout
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.ceil

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [28])
class DetailActionAlignmentTest {
    @Test fun pitch_icon_and_text_share_the_center_of_the_action_face() = checkActions()

    @Test
    @Config(qualifiers = "land-night")
    fun landscape_dark_actions_keep_the_same_alignment() = checkActions()

    private fun checkActions() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup()
        try {
            val activity = controller.get()
            val root = activity.layoutInflater.inflate(R.layout.tagsummaryview, null)
            val key = root.findViewById<CenteredPitchPipeButton>(R.id.playKeyNoteButton)
            val sheet = root.findViewById<MaterialButton>(R.id.sheetMusicLink)
            assertEquals(MaterialButton.ICON_GRAVITY_TEXT_START, sheet.iconGravity)
            val density = activity.resources.displayMetrics.density
            for (widthDp in listOf(180, 320, 480)) {
                for (text in listOf("C", "Minor:G", "Major:Eb")) {
                    key.text = text
                    val width = (widthDp * density).toInt()
                    val height = (48 * density).toInt()
                    key.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
                    )
                    key.layout(0, 0, width, height)
                    val icon = key.compoundDrawablesRelative[0]!!
                    val groupWidth =
                        icon.intrinsicWidth + key.compoundDrawablePadding +
                            ceil(Layout.getDesiredWidth(key.text, key.paint).toDouble()).toInt()
                    assertEquals(
                        "Centered group text=$text width=$width padding=${key.paddingStart}/${key.paddingEnd} icon=${icon.intrinsicWidth} gap=${key.compoundDrawablePadding} group=$groupWidth measured=${key.measuredWidth} font=${key.textSize}",
                        width / 2f,
                        key.paddingStart + groupWidth / 2f,
                        1f,
                    )
                    assertEquals("No extra gap before the note text", 0f, key.layout.getLineLeft(0), 1f)
                    val before = key.paddingStart
                    key.isActivated = true
                    key.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
                    )
                    assertEquals(before, key.paddingStart)
                    key.isActivated = false
                    assertEquals(width, key.width)
                }
            }
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
