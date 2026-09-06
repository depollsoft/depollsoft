package depollsoft.pitchperfect

import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bindroid.trackable.TrackableField

class PitchPipeFragment : Fragment() {
    val model: PitchPipeModel by TrackableField(PitchPipeModel())
    private var instrument: PitchInstrumentView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.activity?.volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootView = inflater.inflate(R.layout.pitchpipeview, container, false)
        instrument = rootView.findViewById(R.id.pitchInstrument)
        instrument?.model = model
        instrument?.toggleMode = SettingsModel.toggleNotes
        return rootView
    }

    override fun onResume() {
        super.onResume()
        instrument?.toggleMode = SettingsModel.toggleNotes
        instrument?.invalidate()
    }

    override fun onPause() {
        super.onPause()
        instrument?.stopAll()
    }

    @Deprecated("Deprecated in Java")
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        instrument?.stopAll()
    }
}
