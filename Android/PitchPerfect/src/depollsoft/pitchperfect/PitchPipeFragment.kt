package depollsoft.pitchperfect

import android.media.AudioManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import com.bindroid.trackable.TrackableField
import com.bindroid.ui.UiBinder
import depollsoft.pitchperfect.converters.PitchPipeNoteTextConverter

class PitchPipeFragment : Fragment() {

    val model: PitchPipeModel by TrackableField(PitchPipeModel())

    val toggle: Boolean
        get() = SettingsModel.toggleNotes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.activity?.volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.pitchpipeview, container, false)

        UiBinder.bind(rootView, R.id.pitchButton0, "Note", this, "Model.Notes[0]")
        UiBinder.bind(rootView, R.id.pitchButton1, "Note", this, "Model.Notes[1]")
        UiBinder.bind(rootView, R.id.pitchButton2, "Note", this, "Model.Notes[2]")
        UiBinder.bind(rootView, R.id.pitchButton3, "Note", this, "Model.Notes[3]")
        UiBinder.bind(rootView, R.id.pitchButton4, "Note", this, "Model.Notes[4]")
        UiBinder.bind(rootView, R.id.pitchButton5, "Note", this, "Model.Notes[5]")
        UiBinder.bind(rootView, R.id.pitchButton6, "Note", this, "Model.Notes[6]")
        UiBinder.bind(rootView, R.id.pitchButton7, "Note", this, "Model.Notes[7]")
        UiBinder.bind(rootView, R.id.pitchButton8, "Note", this, "Model.Notes[8]")
        UiBinder.bind(rootView, R.id.pitchButton9, "Note", this, "Model.Notes[9]")
        UiBinder.bind(rootView, R.id.pitchButton10, "Note", this, "Model.Notes[10]")
        UiBinder.bind(rootView, R.id.pitchButton11, "Note", this, "Model.Notes[11]")

        UiBinder.bind(rootView, R.id.pitchButton0, "IsToggle", this, "Toggle")
        UiBinder.bind(rootView, R.id.pitchButton1, "IsToggle", this, "Toggle")
        UiBinder.bind(rootView, R.id.pitchButton2, "IsToggle", this, "Toggle")
        UiBinder.bind(rootView, R.id.pitchButton3, "IsToggle", this, "Toggle")
        UiBinder.bind(rootView, R.id.pitchButton4, "IsToggle", this, "Toggle")
        UiBinder.bind(rootView, R.id.pitchButton5, "IsToggle", this, "Toggle")
        UiBinder.bind(rootView, R.id.pitchButton6, "IsToggle", this, "Toggle")
        UiBinder.bind(rootView, R.id.pitchButton7, "IsToggle", this, "Toggle")
        UiBinder.bind(rootView, R.id.pitchButton8, "IsToggle", this, "Toggle")
        UiBinder.bind(rootView, R.id.pitchButton9, "IsToggle", this, "Toggle")
        UiBinder.bind(rootView, R.id.pitchButton10, "IsToggle", this, "Toggle")
        UiBinder.bind(rootView, R.id.pitchButton11, "IsToggle", this, "Toggle")

        UiBinder.bind(rootView, R.id.pitchButton0, "Text", this, "Model.Notes[0]",
                PitchPipeNoteTextConverter())
        UiBinder.bind(rootView, R.id.pitchButton1, "Text", this, "Model.Notes[1]",
                PitchPipeNoteTextConverter())
        UiBinder.bind(rootView, R.id.pitchButton2, "Text", this, "Model.Notes[2]",
                PitchPipeNoteTextConverter())
        UiBinder.bind(rootView, R.id.pitchButton3, "Text", this, "Model.Notes[3]",
                PitchPipeNoteTextConverter())
        UiBinder.bind(rootView, R.id.pitchButton4, "Text", this, "Model.Notes[4]",
                PitchPipeNoteTextConverter())
        UiBinder.bind(rootView, R.id.pitchButton5, "Text", this, "Model.Notes[5]",
                PitchPipeNoteTextConverter())
        UiBinder.bind(rootView, R.id.pitchButton6, "Text", this, "Model.Notes[6]",
                PitchPipeNoteTextConverter())
        UiBinder.bind(rootView, R.id.pitchButton7, "Text", this, "Model.Notes[7]",
                PitchPipeNoteTextConverter())
        UiBinder.bind(rootView, R.id.pitchButton8, "Text", this, "Model.Notes[8]",
                PitchPipeNoteTextConverter())
        UiBinder.bind(rootView, R.id.pitchButton9, "Text", this, "Model.Notes[9]",
                PitchPipeNoteTextConverter())
        UiBinder.bind(rootView, R.id.pitchButton10, "Text", this, "Model.Notes[10]",
                PitchPipeNoteTextConverter())
        UiBinder.bind(rootView, R.id.pitchButton11, "Text", this, "Model.Notes[11]",
                PitchPipeNoteTextConverter())

        val fToF = rootView.findViewById(R.id.fToFButton) as RadioButton
        val cToC = rootView.findViewById(R.id.cToCButton) as RadioButton
        fToF.setOnClickListener {
            cToC.isChecked = false
            this@PitchPipeFragment.model.isFromFToF = true
        }
        cToC.setOnClickListener {
            fToF.isChecked = false
            this@PitchPipeFragment.model.isFromFToF = false
        }

        if (this.model.isFromFToF)
            fToF.isChecked = true
        else
            cToC.isChecked = true

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
