package depollsoft.pitchperfect

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.view.View.OnClickListener
import android.widget.Button
import com.bindroid.converters.AdapterConverter
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.TrackableField
import com.bindroid.ui.UiBinder
import com.parse.ParseUser
import depollsoft.lib.compat.ui.MenuItems

class SongListFragment : Fragment() {

    val model: SongsModel by TrackableField(SongsModel.get())

    var editing: Boolean by TrackableField(true)

    private var preparingMenu: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.activity?.volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.songlistview, container, false)

        setHasOptionsMenu(true)

        UiBinder.bind(rootView, R.id.songListView, "Adapter", this, "Model.Songs", AdapterConverter(
                SongListItemView::class.java))

        UiBinder.bind(rootView, R.id.sorryText, "Visibility", this, "Model.Songs[0]",
                BoolConverter.get(true, true))

        val addButton = rootView.findViewById<View>(R.id.addSongButton)
        addButton.setOnClickListener(OnClickListener {
            val i = Intent(this@SongListFragment.context, AddSongActivity::class.java)
            this@SongListFragment.startActivityForResult(i, 1)
        })

        return rootView
    }


    override fun onPrepareOptionsMenu(menu: Menu) {
        if (preparingMenu) {
            return
        }
        preparingMenu = true
        try {
            super.onPrepareOptionsMenu(menu)
            val mi = MenuInflater(this.context)
            mi.inflate(R.menu.songsmenu, menu)

            menu.findItem(R.id.sortMenuItem).setOnMenuItemClickListener {
                SongsModel.get().sortSongs()
                true
            }

            return
        } finally {
            preparingMenu = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        PitchPerfectActivity.handlingResult = true
    }

    override fun onPause() {
        super.onPause()
        for (song in this.model.songs)
            song.stop()
        if (ParseUser.getCurrentUser() != null) {
            SongsModel.get().saveAllToParse()
        }
    }
}
