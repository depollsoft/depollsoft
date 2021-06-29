package depollsoft.pitchperfect

import android.media.AudioManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.bindroid.converters.AdapterConverter
import com.bindroid.trackable.TrackableField
import com.bindroid.ui.UiBinder

class NoteListFragment : Fragment() {
    val model: NoteListModel by TrackableField(NoteListModel())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.notelistview, container, false)

        UiBinder.bind(
                rootView,
                R.id.noteListView,
                "Adapter",
                this,
                "Model.Notes",
                AdapterConverter(NoteListItemView::class.java))

        val list = rootView.findViewById(R.id.noteListView) as ListView
        list.post {
            val totalVisible = list.lastVisiblePosition - list.firstVisiblePosition
            list.setSelection(list.count / 2 - totalVisible / 2)
        }

        return rootView
    }

    private fun stopPlaying() {
        for (n in this.model.notes)
            n.stop()
    }

    override fun onPause() {
        super.onPause()
        stopPlaying()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        stopPlaying()
    }
}