package depollsoft.pitchperfect

import android.app.Activity
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.PitchedSong
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import java.time.Duration

/**
 * Drives the Compose screens from Robolectric tests: launches activities, settles the main looper
 * and Compose's clock, and reads what the semantics tree says.
 */
internal class ComposeScreens(
    private val compose: ComposeTestRule,
) {
    private val controllers = mutableListOf<ActivityController<*>>()

    val latest: ActivityController<*>? get() = controllers.lastOrNull()

    /** Lets posted work and animations finish: the main looper's, then Compose's own clock. */
    fun settle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
    }

    fun <T : Activity> launch(
        type: Class<T>,
        intent: Intent? = null,
    ): ActivityController<T> {
        val controller = if (intent == null) Robolectric.buildActivity(type) else Robolectric.buildActivity(type, intent)
        controllers += controller
        controller.setup()
        settle()
        return controller
    }

    fun launchMain(): PitchPerfectActivity = launch(PitchPerfectActivity::class.java).get()

    /** Taps [tab]'s navigation item and lets the pager settle on it. */
    fun PitchPerfectActivity.show(tab: MainTab) {
        compose.onNodeWithTag(tab.testTag).performClick()
        settle()
    }

    fun tag(tag: String): SemanticsNodeInteraction = compose.onNodeWithTag(tag, useUnmergedTree = false)

    fun exists(tag: String): Boolean = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    fun click(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        settle()
    }

    fun description(tag: String): String? =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()

    fun text(tag: String): String =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.joinToString("").orEmpty()

    fun isSelected(tag: String): Boolean =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected) == true

    /** Closes every activity this test opened, the last through [ScreenTestSupport.finishScreenTest]. */
    fun finish() {
        val open = controllers.toList()
        controllers.clear()
        open.dropLast(1).forEach { runCatching { it.close() } }
        ScreenTestSupport.finishScreenTest(open.lastOrNull())
    }

    companion object {
        fun song(
            title: String,
            key: Key = Key.getMajorKeys()[0],
        ): PitchedSong =
            PitchedSong().apply {
                name = title
                this.key = key
            }
    }
}
