package depollsoft.pitchperfect

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Looper
import android.view.View
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

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

    /**
     * The model's default hook redraws a placed widget: 40ms after the last change, on the render
     * thread, through the real renderer. The range toggle's spoken name says which range it
     * switches to, so it tells an old render from a new one.
     */
    @Test
    fun theAppRedrawsTheWidgetWhenTheRangeChanges() {
        val looper = shadowOf(Looper.getMainLooper())
        val manager = AppWidgetManager.getInstance(context)
        val id = shadowOf(manager).createWidget(PitchPipeAppWidget::class.java, R.layout.pitchpipewidgetview)
        fun toggleSays() = shadowOf(manager).getViewFor(id).findViewById<View>(R.id.rangeToggle).contentDescription?.toString()

        val switchToF = context.getString(R.string.widget_switch_to_f)
        val switchToC = context.getString(R.string.widget_switch_to_c)
        awaitWidget("the placed widget's first render") { toggleSays() == switchToF }

        val model = PitchPipeModel()
        model.isFromFToF = true
        looper.idle()
        assertEquals("nothing redraws before the coalescing delay", switchToF, toggleSays())
        looper.idleFor(Duration.ofMillis(40))
        awaitWidget("the redraw after the range change") { toggleSays() == switchToC }
        model.isFromFToF = false
        looper.idleFor(Duration.ofMillis(40))
    }

    /** Waits for the widget's render thread, which applies its views off the main thread. */
    private fun awaitWidget(
        description: String,
        condition: () -> Boolean,
    ) {
        // Wall-clock time: Robolectric's SystemClock only moves when the looper is idled with time.
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue("timed out waiting for $description", System.currentTimeMillis() < deadline)
            Thread.sleep(10)
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
