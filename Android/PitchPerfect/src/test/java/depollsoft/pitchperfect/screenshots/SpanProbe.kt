package depollsoft.pitchperfect.screenshots

import android.text.StaticLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.NoteText
import depollsoft.pitchperfect.lib.Note
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = RichApplication::class, qualifiers = ScreenshotSupport.PHONE)
class SpanProbe {
    @Test
    fun probe() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val note = Note.getPrunedNotes().first { it.alternate != null }
        val text = NoteText.noteName(note)
        val tv = TextView(context).apply {
            textSize = 24f
            typeface = android.graphics.Typeface.create("sans-serif-condensed", 0)
            this.text = text
        }
        tv.measure(0, 0)
        val paint = android.text.TextPaint(tv.paint)
        for (fallback in listOf(false, true)) {
            val l = StaticLayout.Builder.obtain(text, 0, text.length, paint, 2000).setIncludePad(true)
                .setUseLineSpacingFromFallbacks(fallback).build()
            println("PROBE static fallback=$fallback h=${l.height}")
        }
        println("PROBE view h=${tv.measuredHeight} layout=${tv.layout?.javaClass} fallback=${tv.isFallbackLineSpacing}")
    }
}
