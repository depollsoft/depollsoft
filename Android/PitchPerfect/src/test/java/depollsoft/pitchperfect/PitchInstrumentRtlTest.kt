package depollsoft.pitchperfect

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.ui.PlateTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The face draws and hit-tests in physical pixels whatever the layout direction, as the View did;
 * its screen-reader targets have to sit over the same cells, or exploring by touch in an Arabic or
 * Hebrew locale reads out the note mirrored across the face.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class, qualifiers = "w411dp-h891dp")
class PitchInstrumentRtlTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var widgets: MockedStatic<PitchPipeAppWidget>
    private lateinit var state: PitchInstrumentState

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        ScreenTestSupport.seedSettingsDefaults()
        widgets = Mockito.mockStatic(PitchPipeAppWidget::class.java)
        state = PitchInstrumentState(PitchPipeModel(), haptic = {})
    }

    @After
    fun tearDown() {
        state.stopAll()
        widgets.close()
        Preferences.setTestMode(false)
    }

    @Test
    fun inARightToLeftLayoutEachCellsTargetSitsOverTheCellDrawn() {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                PlateTheme { PitchInstrument(state, Modifier.size(411.dp, 700.dp)) }
            }
        }
        compose.waitForIdle()
        val face = compose.onNodeWithTag(TestTags.PITCH_INSTRUMENT).fetchSemanticsNode().boundsInRoot
        for (index in state.notes.indices) {
            val note: Note = state.notes[index]
            val target =
                compose
                    .onNodeWithContentDescription(NoteNames.spoken(note), useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .boundsInRoot
            val (x, y) = state.geometry.cellCenters[index].let { it[0] to it[1] }
            assertEquals("cell $index across", face.left + x, target.center.x, 1.5f)
            assertEquals("cell $index down", face.top + y, target.center.y, 1.5f)
        }
    }
}
