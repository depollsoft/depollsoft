package depollsoft.tagmaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.bindroid.Binding
import com.bindroid.BindingMode
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.trackable
import com.bindroid.ui.UiBinder
import com.bindroid.utils.Property
import depollsoft.tagmaster.barbershop.RemoteLocation

class TagTracksFragment : Fragment() {
    val parent: TagDetailActivity
        get() = this.activity as TagDetailActivity

    var selectedTrack: RemoteLocation? by trackable()
    private var emptyStateBinding: Binding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.tagtracksview, container, false)
        rootView.findViewById<View>(R.id.scrollView1).applyContentInsets(maxWidthRes = R.dimen.two_column_max_width)

        UiBinder.bind(
            rootView,
            R.id.allPartsButton,
            "Visibility",
            this,
            "Parent.Tag.AllPartsTrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(
            rootView,
            R.id.allPartsButton,
            "Enabled",
            this,
            "Parent.Tag.AllPartsTrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(rootView, R.id.allPartsButton, "Tag", this, "Parent.Tag.AllPartsTrackUri")

        UiBinder.bind(
            rootView,
            R.id.tenorButton,
            "Visibility",
            this,
            "Parent.Tag.TenorTrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(
            rootView,
            R.id.tenorButton,
            "Enabled",
            this,
            "Parent.Tag.TenorTrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(rootView, R.id.tenorButton, "Tag", this, "Parent.Tag.TenorTrackUri")

        UiBinder.bind(
            rootView,
            R.id.leadButton,
            "Visibility",
            this,
            "Parent.Tag.LeadTrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(rootView, R.id.leadButton, "Enabled", this, "Parent.Tag.LeadTrackUri", BoolConverter.get())
        UiBinder.bind(rootView, R.id.leadButton, "Tag", this, "Parent.Tag.LeadTrackUri")

        UiBinder.bind(
            rootView,
            R.id.bariButton,
            "Visibility",
            this,
            "Parent.Tag.BaritoneTrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(
            rootView,
            R.id.bariButton,
            "Enabled",
            this,
            "Parent.Tag.BaritoneTrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(rootView, R.id.bariButton, "Tag", this, "Parent.Tag.BaritoneTrackUri")

        UiBinder.bind(
            rootView,
            R.id.bassButton,
            "Visibility",
            this,
            "Parent.Tag.BassTrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(rootView, R.id.bassButton, "Enabled", this, "Parent.Tag.BassTrackUri", BoolConverter.get())
        UiBinder.bind(rootView, R.id.bassButton, "Tag", this, "Parent.Tag.BassTrackUri")

        UiBinder.bind(
            rootView,
            R.id.other1Button,
            "Visibility",
            this,
            "Parent.Tag.Other1TrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(
            rootView,
            R.id.other1Button,
            "Enabled",
            this,
            "Parent.Tag.Other1TrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(rootView, R.id.other1Button, "Tag", this, "Parent.Tag.Other1TrackUri")

        UiBinder.bind(
            rootView,
            R.id.other2Button,
            "Visibility",
            this,
            "Parent.Tag.Other2TrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(
            rootView,
            R.id.other2Button,
            "Enabled",
            this,
            "Parent.Tag.Other2TrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(rootView, R.id.other2Button, "Tag", this, "Parent.Tag.Other2TrackUri")

        UiBinder.bind(
            rootView,
            R.id.other3Button,
            "Visibility",
            this,
            "Parent.Tag.Other3TrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(
            rootView,
            R.id.other3Button,
            "Enabled",
            this,
            "Parent.Tag.Other3TrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(rootView, R.id.other3Button, "Tag", this, "Parent.Tag.Other3TrackUri")

        UiBinder.bind(
            rootView,
            R.id.other4Button,
            "Visibility",
            this,
            "Parent.Tag.Other4TrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(
            rootView,
            R.id.other4Button,
            "Enabled",
            this,
            "Parent.Tag.Other4TrackUri",
            BoolConverter.get(),
        )
        UiBinder.bind(rootView, R.id.other4Button, "Tag", this, "Parent.Tag.Other4TrackUri")

        UiBinder.bind(rootView, R.id.trackNotesTextView, "Text", this, "Parent.Tag.RecordingMethod")
        UiBinder.bind(
            rootView,
            R.id.trackNotesLayout,
            "Visibility",
            this,
            "Parent.Tag.RecordingMethod",
            BoolConverter.get(),
        )

        UiBinder.bind(rootView, R.id.mediaPlayer, "RemoteLocation", this, "SelectedTrack")
        UiBinder.bind(rootView, R.id.mediaPlayer, "Enabled", this, "SelectedTrack", BoolConverter.get())

        val group = rootView.findViewById<RadioGroup>(R.id.partsRadioGroup)
        group.setOnCheckedChangeListener { g, checkedId ->
            val rb = g.findViewById<RadioButton>(checkedId)
            selectedTrack = rb?.tag as? RemoteLocation
        }

        // A missing selection or an in-flight/failed download is not an empty catalog.
        // Observe the loaded tag and its track fields, including same-ID replacements.
        emptyStateBinding =
            UiBinder.bind(
                Property<Boolean>(null, { empty ->
                    if (empty) {
                        // Cancel preparation/playback before the transport becomes unreachable.
                        selectedTrack = null
                        group.clearCheck()
                        rootView.findViewById<MediaPlayerView>(R.id.mediaPlayer).apply {
                            remoteLocation = null
                            stop()
                        }
                    }
                    rootView.findViewById<View>(R.id.sorryTextView).visibility = if (empty) View.VISIBLE else View.GONE
                    rootView.findViewById<View>(R.id.mediaPlayer).visibility = if (empty) View.GONE else View.VISIBLE
                    group.visibility = if (empty) View.GONE else View.VISIBLE
                }, Boolean::class.java),
                Property<Boolean>({ parent.tag?.tracks?.isEmpty() == true }, null, Boolean::class.java),
                BindingMode.ONE_WAY,
            )

        return rootView
    }

    override fun onDestroyView() {
        emptyStateBinding = null
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        stopMedia()
    }

    private fun stopMedia() {
        this.view?.findViewById<MediaPlayerView>(R.id.mediaPlayer)?.stop()
    }
}
