package depollsoft.pitchperfect.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import depollsoft.pitchperfect.TestTags

/** One entry of an overflow menu. */
class PlateMenuItem(
    val title: String,
    val testTag: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * Presses of the keyboard's Menu key, which an activity forwards from `onKeyUp`; the screen's
 * overflow menu opens on each, as the window action bar's did.
 */
@Stable
class MenuKeySignal {
    var presses by mutableIntStateOf(0)
        private set

    fun press() {
        presses++
    }
}

val LocalMenuKey = staticCompositionLocalOf { MenuKeySignal() }

/**
 * The action bar's overflow button and its menu, as the window action bar's "More options"
 * button: 40dp wide, the three-dot icon 6dp in from its leading edge. The menu opens over the
 * button, and the Menu key opens it too.
 */
@Composable
fun PlateOverflowMenu(items: List<PlateMenuItem>) {
    val colors = plateColors
    var open by remember { mutableStateOf(false) }
    val description = stringResource(androidx.appcompat.R.string.abc_action_menu_overflow_description)
    val menuKey = LocalMenuKey.current
    val pressesAtStart = remember { menuKey.presses }
    LaunchedEffect(menuKey.presses) { if (menuKey.presses != pressesAtStart) open = true }
    WithTooltip(description) { showTooltip ->
        Box(
            Modifier
                .width(40.dp)
                .height(48.dp)
                .testTag(TestTags.OVERFLOW)
                .combinedClickable(
                    role = Role.Button,
                    interactionSource = null,
                    indication = ripple(bounded = false, radius = 20.dp),
                    onLongClick = showTooltip,
                ) { open = true }
                .semantics { contentDescription = description }
                .padding(start = 6.dp, end = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            DrawableIcon(androidx.appcompat.R.drawable.abc_ic_menu_overflow_material, colors.ink)
        }
        PlatePopupMenu(
            open,
            { open = false },
            items.map { PopupMenuItem(it.title, it.enabled, it.testTag, it.onClick) },
            placement = MenuPlacement.OVER_ANCHOR_END,
        )
    }
}
