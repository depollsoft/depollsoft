package depollsoft.pitchperfect.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
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

/** A menu row: body text in ink, dimmed when it cannot act. */
@Composable
fun PlateMenuRow(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = plateColors
    DropdownMenuItem(onClick, modifier, enabled = enabled) {
        PlateText(text, style = plateText(16.sp, if (enabled) colors.ink else colors.ink.copy(alpha = DISABLED_ALPHA)))
    }
}

/**
 * The action bar's overflow button and its menu, as the window action bar's "More options"
 * button: 40dp wide, the three-dot icon 6dp in from its leading edge.
 */
@Composable
fun PlateOverflowMenu(items: List<PlateMenuItem>) {
    val colors = plateColors
    var open by remember { mutableStateOf(false) }
    val description = stringResource(androidx.appcompat.R.string.abc_action_menu_overflow_description)
    Box {
        Box(
            Modifier
                .width(40.dp)
                .height(48.dp)
                .testTag(TestTags.OVERFLOW)
                .clickable(
                    role = Role.Button,
                    interactionSource = null,
                    indication = ripple(bounded = false, radius = 20.dp),
                ) { open = true }
                .semantics { contentDescription = description }
                .padding(start = 6.dp, end = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            DrawableIcon(androidx.appcompat.R.drawable.abc_ic_menu_overflow_material, colors.ink)
        }
        DropdownMenu(open, { open = false }) {
            items.forEach { item ->
                PlateMenuRow(item.title, Modifier.testTag(item.testTag), item.enabled) {
                    open = false
                    item.onClick()
                }
            }
        }
    }
}

private const val DISABLED_ALPHA = 0.38f
