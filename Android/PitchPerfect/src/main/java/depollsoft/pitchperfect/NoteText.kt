package depollsoft.pitchperfect

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.Note

/**
 * How notes and keys are spelled on screen: letter names with engraved accidentals from the
 * NoteHedz face, octaves as subscripts, and key signatures drawn with the MusiQwik staff font.
 */
object NoteText {
    /** NoteHedz's sharp and flat glyphs sit on these code points. */
    const val SHARP = CommonModel.sharpString
    const val FLAT = CommonModel.flatString

    private fun family(typeface: android.graphics.Typeface?): FontFamily? = typeface?.let { FontFamily(Typeface(it)) }

    private val noteHedz: FontFamily? get() = family(CommonModel.getNoteHedz())
    private val musiQwik: FontFamily? get() = family(CommonModel.getMusiQwik())

    private fun AnnotatedString.Builder.appendAccidental(accidental: Accidental) {
        val glyph =
            when (accidental) {
                Accidental.Natural -> return
                Accidental.Flat -> FLAT
                Accidental.Sharp -> SHARP
            }
        withStyle(SpanStyle(fontFamily = noteHedz, fontSize = 1.2.em)) { append(glyph) }
    }

    private fun AnnotatedString.Builder.appendNote(note: Note) {
        append(note.friendlyName)
        appendAccidental(note.accidental)
        withStyle(SpanStyle(fontSize = 0.68.em, baselineShift = BaselineShift.Subscript)) {
            append(note.octave.toString())
        }
    }

    /** A Notes row: "C♯₄ / D♭₄", or one spelling for a natural. */
    fun noteName(note: Note): AnnotatedString =
        buildAnnotatedString {
            appendNote(note)
            note.alternate?.let {
                append(" / ")
                appendNote(it)
            }
        }

    /** A key's name: its tonic, upper case for major and lower case for minor, with its accidental. */
    fun keyName(key: Key): AnnotatedString =
        buildAnnotatedString {
            append(key.friendlyName)
            appendAccidental(key.accidental)
        }

    /** A key signature: a treble clef and the key's sharps or flats, in the staff font. */
    fun keySignature(key: Key): AnnotatedString =
        buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 1.7.em)) {
                withStyle(SpanStyle(fontFamily = musiQwik)) { append('&') }
                val count = key.numAccidentals
                val glyph =
                    when {
                        count > 0 -> '¡' + count - 1
                        count == -6 -> '€'
                        count < 0 -> '¨' - count - 1
                        else -> null
                    }
                if (glyph != null) withStyle(SpanStyle(fontFamily = musiQwik)) { append(glyph) }
            }
        }
}
