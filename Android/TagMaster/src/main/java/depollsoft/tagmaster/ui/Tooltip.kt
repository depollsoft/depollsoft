package depollsoft.tagmaster.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import depollsoft.compose.LabelTooltipState
import depollsoft.compose.rememberLabelTooltipState
import depollsoft.compose.tooltipOnHover
import kotlinx.coroutines.awaitCancellation

/**
 * Names an icon-only control in a Material plain tooltip, shown on a long press and on mouse
 * hover (see [LabelTooltipState]). [content] wires the state's `longPressed` into its long click.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithTooltip(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable (LabelTooltipState) -> Unit,
) {
    val tooltip = rememberLabelTooltipState()
    val material = rememberTooltipState(isPersistent = true)
    LaunchedEffect(tooltip.visible) {
        if (!tooltip.visible) return@LaunchedEffect
        try {
            material.show()
            awaitCancellation()
        } finally {
            material.dismiss()
        }
    }
    // The caller's modifier (a layoutId, say) goes on this Box: TooltipBox does not put its own
    // modifier on the node a parent layout measures.
    Box(modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
            tooltip = { PlainTooltip { Text(label, Modifier.testTag("tooltip")) } },
            state = material,
            focusable = false,
            enableUserInput = false,
        ) {
            Box(Modifier.tooltipOnHover(tooltip)) { content(tooltip) }
        }
    }
}
