package depollsoft.pitchperfect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import depollsoft.pitchperfect.converters.KeyNameConverter
import depollsoft.pitchperfect.lib.PitchedSong

/**
 * "Add songs": a sectioned checklist of every song the current set list does not already have.
 *
 * One section per other list, in list order; songs whose title (ignoring case) and key already
 * exist in the target are left out entirely rather than shown disabled. Confirming appends deep
 * copies with fresh ids, in the order the sections present them.
 */
class AddSongsFromListActivity : AppCompatActivity() {
    private val model get() = SongsModel.get()
    private lateinit var targetId: String
    private lateinit var adapter: AddableAdapter
    private var confirmButton: MaterialButton? = null
    private var confirmMenuItem: MenuItem? = null
    private var selectAllMenuItem: MenuItem? = null

    /** The songs ticked so far; [PitchedSong] equality is by id, which is unique per song. */
    private val selection = linkedSetOf<PitchedSong>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.addsongsfromlistview)
        setTitle(R.string.AddSongsTitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        targetId = intent.getStringExtra(LIST_EXTRA) ?: model.currentListId

        val rows = buildRows()
        adapter = AddableAdapter(rows)
        val list = findViewById<RecyclerView>(R.id.addableSongList)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(this, R.drawable.divider_hairline)?.let { divider.setDrawable(it) }
        list.addItemDecoration(divider)

        confirmButton =
            findViewById<MaterialButton>(R.id.addSongsConfirmButton).also {
                it.setOnClickListener { confirm() }
            }
        if (rows.isEmpty()) {
            list.visibility = View.GONE
            findViewById<TextView>(R.id.addableEmptyText).visibility = View.VISIBLE
        }
        syncConfirmAction()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.addsongsmenu, menu)
        confirmMenuItem = menu.findItem(R.id.confirmAddSongsMenuItem)
        selectAllMenuItem = menu.findItem(R.id.selectAllSongsMenuItem)
        syncConfirmAction()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.confirmAddSongsMenuItem -> {
                confirm()
                true
            }

            R.id.selectAllSongsMenuItem -> {
                toggleSelectAll()
                true
            }

            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    /** Copies the ticked songs into the target list and closes. Visible for the screen tests. */
    internal fun confirm() {
        if (selection.isEmpty()) return
        val ordered = adapter.songRows().map { it.song }.filter { it in selection }
        model.copySongs(ordered, targetId)
        PitchPerfectActivity.handlingResult = true
        setResult(RESULT_OK)
        finish()
    }

    /** The current tick count, for the screen tests. */
    internal val selectedCount: Int get() = selection.size

    /** Ticks every offered song, or clears the ticks once they are all in. */
    internal fun toggleSelectAll() {
        val all = adapter.songRows().map { it.song }
        if (all.isEmpty()) return
        if (selection.size == all.size) selection.clear() else selection.addAll(all)
        adapter.notifyDataSetChanged()
        syncConfirmAction()
    }

    internal fun toggle(song: PitchedSong) {
        if (!selection.remove(song)) selection.add(song)
        adapter.notifyDataSetChanged()
        syncConfirmAction()
    }

    private fun syncConfirmAction() {
        val count = selection.size
        val label =
            if (count == 0) {
                getString(R.string.AddSongsConfirm)
            } else {
                resources.getQuantityString(R.plurals.AddSongsCount, count, count)
            }
        confirmButton?.text = label
        confirmButton?.isEnabled = count > 0
        confirmMenuItem?.isEnabled = count > 0
        confirmMenuItem?.title = label
        // Vector menu icons do not dim on their own; a dead checkmark must not read as live.
        confirmMenuItem?.icon?.alpha = if (count > 0) 255 else DISABLED_ICON_ALPHA
        val total = adapter.songRows().size
        selectAllMenuItem?.isVisible = total > 0
        selectAllMenuItem?.setTitle(
            if (total > 0 && count == total) R.string.AddSongsClearSelection else R.string.AddSongsSelectAll,
        )
    }

    private fun buildRows(): List<Row> =
        model.addableSongs(targetId).flatMap { (list, songs) ->
            listOf<Row>(Row.Section(model.displayName(list))) + songs.map { Row.Song(it) }
        }

    private sealed class Row {
        data class Section(
            val title: String,
        ) : Row()

        data class Song(
            val song: PitchedSong,
        ) : Row()
    }

    private inner class AddableAdapter(
        private val rows: List<Row>,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        fun songRows(): List<Row.Song> = rows.filterIsInstance<Row.Song>()

        override fun getItemCount(): Int = rows.size

        override fun getItemViewType(position: Int): Int =
            if (rows[position] is Row.Section) TYPE_SECTION else TYPE_SONG

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_SECTION) {
                SectionHolder(inflater.inflate(R.layout.addablesongsectionview, parent, false))
            } else {
                SongHolder(inflater.inflate(R.layout.addablesongitemview, parent, false))
            }
        }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) {
            when (val row = rows[position]) {
                is Row.Section -> (holder as SectionHolder).title.text = row.title
                is Row.Song -> {
                    val songHolder = holder as SongHolder
                    val song = row.song
                    songHolder.title.text = song.name
                    songHolder.key.text =
                        KeyNameConverter()
                            .convertToTarget(song.key, CharSequence::class.java) as? CharSequence
                    val ticked = song in selection
                    songHolder.check.visibility = if (ticked) View.VISIBLE else View.INVISIBLE
                    songHolder.itemView.isSelected = ticked
                    songHolder.itemView.tag = song.id
                    songHolder.itemView.setOnClickListener { toggle(song) }
                    // A row is a checkbox to a screen reader: it has a checked state, not a
                    // selected one, and toggling it announces the change.
                    ViewCompat.setAccessibilityDelegate(
                        songHolder.itemView,
                        object : AccessibilityDelegateCompat() {
                            override fun onInitializeAccessibilityNodeInfo(
                                host: View,
                                info: AccessibilityNodeInfoCompat,
                            ) {
                                super.onInitializeAccessibilityNodeInfo(host, info)
                                info.className = android.widget.CheckBox::class.java.name
                                info.isCheckable = true
                                info.isChecked = song in selection
                            }
                        },
                    )
                }
            }
        }
    }

    private class SectionHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.sectionHeaderText)
    }

    private class SongHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.addableSongTitle)
        val key: TextView = view.findViewById(R.id.addableSongKey)
        val check: ImageView = view.findViewById(R.id.addableSongCheck)
    }

    companion object {
        const val LIST_EXTRA = "depollsoft.pitchperfect.AddSongsFromList.listId"
        private const val DISABLED_ICON_ALPHA = 97
        private const val TYPE_SECTION = 0
        private const val TYPE_SONG = 1
    }
}
