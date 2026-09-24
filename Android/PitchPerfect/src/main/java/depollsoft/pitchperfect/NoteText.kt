package depollsoft.pitchperfect

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.SubscriptSpan
import depollsoft.lib.ui.CustomTypefaceSpan
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.Note

/**
 * How notes and keys are spelled on screen: letter names with engraved accidentals from the
 * NoteHedz face, octaves as subscripts, and key signatures drawn with the MusiQwik staff font.
 *
 * They are Android spans rather than Compose styles because the rows lay them out with
 * [depollsoft.pitchperfect.ui.LegacyText]: a subscript span leaves the line as tall as TextView
 * made it, where Compose's baseline shift would grow the row.
 */
object NoteText {
    /** NoteHedz's sharp and flat glyphs sit on these code points. */
    const val SHARP = CommonModel.sharpString
    const val FLAT = CommonModel.flatString

    private fun SpannableStringBuilder.styleLast(vararg spans: Any) {
        spans.forEach { setSpan(it, length - 1, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
    }

    private fun SpannableStringBuilder.appendAccidental(accidental: Accidental) {
        val glyph =
            when (accidental) {
                Accidental.Natural -> return
                Accidental.Flat -> FLAT
                Accidental.Sharp -> SHARP
            }
        append(glyph)
        styleLast(CustomTypefaceSpan("NoteHedz", CommonModel.getNoteHedz()), RelativeSizeSpan(1.2f))
    }

    private fun SpannableStringBuilder.appendNote(note: Note) {
        append(note.friendlyName)
        appendAccidental(note.accidental)
        append(note.octave.toString())
        styleLast(RelativeSizeSpan(0.68f), SubscriptSpan())
    }

    /** A Notes row: "C♯₄ / D♭₄", or one spelling for a natural. */
    fun noteName(note: Note): CharSequence =
        SpannableStringBuilder().apply {
            appendNote(note)
            note.alternate?.let {
                append(" / ")
                appendNote(it)
            }
        }

    /** A key's name: its tonic, upper case for major and lower case for minor, with its accidental. */
    fun keyName(key: Key): CharSequence =
        SpannableStringBuilder().apply {
            append(key.friendlyName)
            appendAccidental(key.accidental)
        }

    /** A key signature: a treble clef and the key's sharps or flats, in the staff font. */
    fun keySignature(key: Key): CharSequence =
        SpannableStringBuilder().apply {
            val staff = CommonModel.getMusiQwik()
            append('&')
            styleLast(CustomTypefaceSpan("MusiQwik", staff))
            val count = key.numAccidentals
            val glyph =
                when {
                    count > 0 -> '¡' + count - 1
                    count == -6 -> '€'
                    count < 0 -> '¨' - count - 1
                    else -> null
                }
            if (glyph != null) {
                append(glyph)
                styleLast(CustomTypefaceSpan("MusiQwik", staff))
            }
            setSpan(RelativeSizeSpan(1.7f), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
}
