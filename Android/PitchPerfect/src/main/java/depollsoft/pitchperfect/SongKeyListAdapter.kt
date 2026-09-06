package depollsoft.pitchperfect

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import depollsoft.pitchperfect.converters.KeyNameConverter
import depollsoft.pitchperfect.converters.KeySignatureConverter
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.KeyType

/**
 * The song editor's key picker: the Keys screen's signature list, with the
 * chosen row lit. Choosing a key is silent; the pitch pipe and Keys screens
 * are where notes sound.
 */
class SongKeyListAdapter(
    initialKey: Key,
    private val onKeyChange: (Key) -> Unit,
) : RecyclerView.Adapter<SongKeyListAdapter.RowHolder>() {
    class RowHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val signature: TextView = view.findViewById(R.id.keySignatureTextView)
        val name: TextView = view.findViewById(R.id.keyNameTextView)
    }

    private val signatureConverter = KeySignatureConverter()
    private val nameConverter = KeyNameConverter()
    var selectedKey: Key = initialKey
        private set

    val isMinor: Boolean
        get() = selectedKey.keyType == KeyType.Minor

    val keys: List<Key>
        get() = if (isMinor) Key.getMinorKeys() else Key.getMajorKeys()

    val selectedIndex: Int
        get() = keys.indexOfFirst { it == selectedKey }

    /** Selecting from code never sounds the tonic; only a tap does. */
    fun select(key: Key) {
        val modeChanged = (key.keyType == KeyType.Minor) != isMinor
        selectedKey = key
        if (modeChanged) notifyDataSetChanged() else notifyItemRangeChanged(0, itemCount)
    }

    /** Keep the same signature when the mode flips: a relative key shares it. */
    fun setMinor(minor: Boolean) {
        if (minor == isMinor) return
        val accidentals = selectedKey.numAccidentals
        val list = if (minor) Key.getMinorKeys() else Key.getMajorKeys()
        val match = list.firstOrNull { it.numAccidentals == accidentals } ?: list[list.size / 2]
        selectedKey = match
        notifyDataSetChanged()
        onKeyChange(match)
    }

    override fun getItemCount(): Int = keys.size

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RowHolder = RowHolder(LayoutInflater.from(parent.context).inflate(R.layout.songkeyrowview, parent, false))

    override fun onBindViewHolder(
        holder: RowHolder,
        position: Int,
    ) {
        val key = keys[position]
        val selected = key == selectedKey
        holder.signature.text = signatureConverter.convertToTarget(key, CharSequence::class.java) as CharSequence
        holder.name.text = nameConverter.convertToTarget(key, CharSequence::class.java) as CharSequence
        // The row's selectors light on selected, which a tap's press cycle
        // never clears; a second tap on the chosen row leaves it chosen.
        holder.itemView.isSelected = selected
        holder.itemView.contentDescription = spokenName(key)
        holder.itemView.setOnClickListener { view ->
            if (key == selectedKey) return@setOnClickListener
            val previous = selectedIndex
            selectedKey = key
            if (previous >= 0) notifyItemChanged(previous)
            notifyItemChanged(position)
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onKeyChange(key)
        }
    }

    companion object {
        @JvmStatic
        fun accidentalCount(numAccidentals: Int): String =
            when {
                numAccidentals == 0 -> "no sharps or flats"
                numAccidentals == 1 -> "1 sharp"
                numAccidentals == -1 -> "1 flat"
                numAccidentals > 0 -> "$numAccidentals sharps"
                else -> "${-numAccidentals} flats"
            }

        @JvmStatic
        fun spokenName(key: Key): String {
            val letter = key.note.friendlyName.uppercase()
            val spelled =
                when (key.accidental) {
                    Accidental.Sharp -> "$letter sharp"
                    Accidental.Flat -> "$letter flat"
                    else -> letter
                }
            val mode = if (key.keyType == KeyType.Minor) "minor" else "major"
            return "$spelled $mode, ${accidentalCount(key.numAccidentals)}"
        }
    }
}
