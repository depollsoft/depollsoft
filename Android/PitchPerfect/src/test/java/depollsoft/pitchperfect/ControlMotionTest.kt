package depollsoft.pitchperfect

import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.ui.PlateBottomNavigation
import depollsoft.pitchperfect.ui.PlateDestination
import depollsoft.pitchperfect.ui.PlateExtendedFab
import depollsoft.pitchperfect.ui.PlateTheme
import depollsoft.pitchperfect.ui.listViewScrollbar
import depollsoft.pitchperfect.ui.pressLift
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/** How the plate's controls move: buttons lift, the Keys switch comes and goes, scrollbars fade. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = RichApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
class ControlMotionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aButtonLiftsWhilePressedAndSettlesBack() {
        val interaction = MutableInteractionSource()
        var lift = 0.dp
        var flat = 0.dp
        compose.setContent {
            lift = pressLift(interaction, 2.dp)
            flat = pressLift(MutableInteractionSource(), 0.dp)
        }
        compose.waitForIdle()
        assertEquals(2.dp, lift)
        val press = PressInteraction.Press(Offset.Zero)
        runBlocking { interaction.emit(press) }
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        assertEquals("6dp above its resting 2dp", 8.dp, lift)
        runBlocking { interaction.emit(PressInteraction.Release(press)) }
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        assertEquals(2.dp, lift)
        assertEquals("a flat button stays flat", 0.dp, flat)
    }

    @Test
    fun theKeysSwitchGrowsInFromEightyPercentAndJustFadesOut() {
        var visible by mutableStateOf(false)
        compose.setContent {
            PlateTheme {
                PlateExtendedFab(R.drawable.ic_keys, "Minor", "Show minor keys", {}, Modifier.testTag("fab"), visible = visible)
            }
        }
        compose.waitForIdle()
        visible = true
        compose.waitForIdle()
        val full = compose.onNodeWithContentDescription("Show minor keys").fetchSemanticsNode().boundsInRoot.width
        visible = false
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        visible = true
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        val first = compose.onNodeWithContentDescription("Show minor keys").fetchSemanticsNode().boundsInRoot.width
        assertTrue("starts at 80% or more of its size, not from nothing: $first of $full", first >= full * 0.79f && first < full)
        compose.mainClock.advanceTimeBy(300)
        visible = false
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeByFrame()
        assertEquals("it does not shrink as it goes", full, compose.onNodeWithContentDescription("Show minor keys").fetchSemanticsNode().boundsInRoot.width, 1f)
        compose.mainClock.advanceTimeBy(120)
        assertTrue("gone after the 75ms fade", compose.onAllNodesWithTag("fab").fetchSemanticsNodes().isEmpty())
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun aListShowsItsScrollbarAsItAppearsThenFadesItAsAListViewDid() {
        compose.setContent {
            val state = rememberLazyListState()
            PlateTheme {
                LazyColumn(Modifier.size(200.dp).background(Color.White).listViewScrollbar(state).testTag("list"), state = state) {
                    items(100) { Box(Modifier.fillMaxWidth().height(40.dp)) }
                }
            }
        }
        compose.waitForIdle()
        assertTrue("the thumb shows at first", thumbDrawn())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_300))
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        assertFalse("and fades once the list is still", thumbDrawn())
    }

    /** Whether anything but white is drawn along the list's trailing edge. */
    private fun thumbDrawn(): Boolean {
        val pixels = compose.onNodeWithTag("list").captureToImage().toPixelMap()
        val x = pixels.width - 3
        return (0 until pixels.height).any { pixels[x, it] != Color.White }
    }

    @Test
    fun theNavigationMirrorsRightToLeft() {
        val destinations = MainTab.entries.map { PlateDestination(it.name, it.icon, it.testTag) }
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                PlateTheme { PlateBottomNavigation(destinations, 0, {}) }
            }
        }
        compose.waitForIdle()
        fun left(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.left
        assertTrue("the first tab is at the right", left(TestTags.TAB_PITCH_PIPE) > left(TestTags.TAB_SONGS))
    }

    private val Dp.px get() = value
}
