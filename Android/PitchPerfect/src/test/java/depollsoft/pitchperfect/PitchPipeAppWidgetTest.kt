package depollsoft.pitchperfect

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The home-screen widget's taps, and the app redrawing it when the pitch pipe's range changes. */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class)
class PitchPipeAppWidgetTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val sounding = mutableSetOf<Note>()

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        ScreenTestSupport.seedSettingsDefaults()
        Note.setPlayer(
            object : Note.NotePlayer {
                override fun play(n: Note) {
                    sounding += n
                }

                override fun stop(n: Note) {
                    sounding -= n
                }
            },
        )
    }

    @After
    fun tearDown() {
        Note.getCommonNotes().forEach { it.stop() }
        Note.setPlayer(ScreenTestSupport.silentPlayer)
        Preferences.setTestMode(false)
    }

    private fun receive(intent: Intent) = PitchPipeAppWidget().onReceive(context, intent)

    private fun tapNote(
        name: String,
        accidental: String? = null,
    ) = receive(
        Intent(PitchPipeAppWidget.ACTION_TOGGLE_NOTE)
            .putExtra(PitchPipeAppWidget.EXTRA_NOTE_NAME, name)
            .putExtra(PitchPipeAppWidget.EXTRA_OCTAVE, 4)
            .apply { if (accidental != null) putExtra(PitchPipeAppWidget.EXTRA_ACCIDENTAL, accidental) },
    )

    @Test
    fun theAppRedrawsTheWidgetWhenTheRangeChanges() {
        Mockito.mockStatic(PitchPipeAppWidget::class.java).use { widgets ->
            val model = PitchPipeModel()
            widgets.verifyNoInteractions()
            model.isFromFToF = true
            widgets.verify { PitchPipeAppWidget.updateWidgets() }
        }
    }

    @Test
    fun aTapTogglesItsNote() {
        val sharp = Note.findNote("C", Accidental.Sharp, 4)
        tapNote("C", "#")
        assertEquals(setOf(sharp), sounding)
        tapNote("C", "#")
        assertTrue(sounding.isEmpty())
    }

    @Test
    fun theRangeSwitchStopsThePipeAndMovesIt() {
        tapNote("A")
        assertFalse(sounding.isEmpty())
        receive(Intent(PitchPipeAppWidget.ACTION_SET_RANGE).putExtra(PitchPipeAppWidget.EXTRA_HIGH, true))
        assertTrue("switching range stops the pipe", sounding.isEmpty())
        assertTrue(PitchPipeModel(updateWidgets = {}).isFromFToF)
    }
}
