package depollsoft.tagmaster

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.TrackableField
import com.bindroid.trackable.trackable
import com.bindroid.ui.UiBinder
import depollsoft.tagmaster.barbershop.RemoteLocation

class TagTracksFragment : Fragment() {
    val parent: TagDetailActivity
        get() = this.activity as TagDetailActivity

    var selectedTrack: RemoteLocation? by trackable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.tagtracksview, container, false)

        UiBinder.bind(rootView, R.id.allPartsButton, "Visibility", this, "Parent.Tag.AllPartsTrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.allPartsButton, "Enabled", this, "Parent.Tag.AllPartsTrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.allPartsButton, "Tag", this, "Parent.Tag.AllPartsTrackUri")

        UiBinder.bind(rootView, R.id.tenorButton, "Visibility", this, "Parent.Tag.TenorTrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.tenorButton, "Enabled", this, "Parent.Tag.TenorTrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.tenorButton, "Tag", this, "Parent.Tag.TenorTrackUri")

        UiBinder.bind(rootView, R.id.leadButton, "Visibility", this, "Parent.Tag.LeadTrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.leadButton, "Enabled", this, "Parent.Tag.LeadTrackUri", BoolConverter.get())
        UiBinder.bind(rootView, R.id.leadButton, "Tag", this, "Parent.Tag.LeadTrackUri")

        UiBinder.bind(rootView, R.id.bariButton, "Visibility", this, "Parent.Tag.BaritoneTrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.bariButton, "Enabled", this, "Parent.Tag.BaritoneTrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.bariButton, "Tag", this, "Parent.Tag.BaritoneTrackUri")

        UiBinder.bind(rootView, R.id.bassButton, "Visibility", this, "Parent.Tag.BassTrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.bassButton, "Enabled", this, "Parent.Tag.BassTrackUri", BoolConverter.get())
        UiBinder.bind(rootView, R.id.bassButton, "Tag", this, "Parent.Tag.BassTrackUri")

        UiBinder.bind(rootView, R.id.other1Button, "Visibility", this, "Parent.Tag.Other1TrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.other1Button, "Enabled", this, "Parent.Tag.Other1TrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.other1Button, "Tag", this, "Parent.Tag.Other1TrackUri")

        UiBinder.bind(rootView, R.id.other2Button, "Visibility", this, "Parent.Tag.Other2TrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.other2Button, "Enabled", this, "Parent.Tag.Other2TrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.other2Button, "Tag", this, "Parent.Tag.Other2TrackUri")

        UiBinder.bind(rootView, R.id.other3Button, "Visibility", this, "Parent.Tag.Other3TrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.other3Button, "Enabled", this, "Parent.Tag.Other3TrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.other3Button, "Tag", this, "Parent.Tag.Other3TrackUri")

        UiBinder.bind(rootView, R.id.other4Button, "Visibility", this, "Parent.Tag.Other4TrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.other4Button, "Enabled", this, "Parent.Tag.Other4TrackUri",
                BoolConverter.get())
        UiBinder.bind(rootView, R.id.other4Button, "Tag", this, "Parent.Tag.Other4TrackUri")

        UiBinder.bind(rootView, R.id.trackNotesTextView, "Text", this, "Parent.Tag.RecordingMethod")
        UiBinder.bind(rootView, R.id.trackNotesLayout, "Visibility", this, "Parent.Tag.RecordingMethod",
                BoolConverter.get())

        UiBinder.bind(rootView, R.id.sorryTextView, "Visibility", this, "Parent.Tag.Tracks",
                BoolConverter.get(true, true))

        UiBinder.bind(rootView, R.id.mediaPlayer, "RemoteLocation", this, "SelectedTrack")
        UiBinder.bind(rootView, R.id.mediaPlayer, "Enabled", this, "SelectedTrack", BoolConverter.get())

        val group = rootView.findViewById<RadioGroup>(R.id.partsRadioGroup)
        group.setOnCheckedChangeListener { g, checkedId ->
            val rb = g.findViewById(checkedId) as RadioButton
            selectedTrack = rb.tag as RemoteLocation
        }

        return rootView
    }

    override fun onStop() {
        super.onStop()
        stopMedia()
    }

    private fun stopMedia() {
        this.view?.findViewById<MediaPlayerView>(R.id.mediaPlayer)?.stop()
    }

}
