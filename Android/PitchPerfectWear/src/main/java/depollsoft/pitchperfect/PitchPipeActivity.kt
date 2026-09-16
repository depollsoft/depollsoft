package depollsoft.pitchperfect

import android.media.AudioManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PitchPipeActivity : AppCompatActivity() {
    private lateinit var instrument: WearPitchInstrumentView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContentView(R.layout.activity_pitch_pipe)
        instrument = findViewById(R.id.pitchInstrument)
        instrument.model = PitchPipeModel()
        instrument.toggleMode = SettingsModel.getToggleNotes()
    }

    override fun onResume() {
        super.onResume()
        instrument.toggleMode = SettingsModel.getToggleNotes()
        instrument.requestFocus()
    }

    override fun onPause() {
        instrument.stopAll()
        super.onPause()
    }
}
