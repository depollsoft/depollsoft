package depollsoft.tagmaster.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import depollsoft.tagmaster.ui.TagMasterType.withoutLineHeight

/** One dialog button: a text button, optionally in the error color for a destructive choice. */
data class DialogButton(
    val text: String,
    val id: String = "dialogButton:$text",
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * A Material 3 alert dialog laid out as MaterialAlertDialogBuilder laid one out: a 28dp-cornered
 * card inset 24dp from the window's sides, the title, an optional message, custom [content], and
 * the buttons right-aligned at the bottom.
 */
@Composable
fun TagMasterDialog(
    onDismissRequest: () -> Unit,
    title: String?,
    message: String? = null,
    confirm: DialogButton? = null,
    dismiss: DialogButton? = null,
    modifier: Modifier = Modifier,
    /**
     * Use AppCompat's minimum dialog window (95% of a portrait screen) instead of the full width:
     * a dialog whose rows and texts only fill the width they are given opens at that minimum.
     */
    wrapWidth: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = TagMasterTheme.colors
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier
                .then(
                    if (wrapWidth) {
                        Modifier.width(minOf(screenWidth * 0.95f, screenWidth) - 48.dp)
                    } else {
                        Modifier
                            .widthIn(max = screenWidth - 48.dp)
                            .fillMaxWidth()
                    },
                )
                .background(colors.surfaceContainerHigh, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .semantics { if (title != null) paneTitle = title }
                .testTag("dialog"),
        ) {
            if (title != null) {
                DialogTitle(title, Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp))
                if (message != null) Spacer(Modifier.height(16.dp))
            }
            // The message and content get only the height the title and buttons leave, as
            // AlertDialog's content panel did, so the buttons stay on a short screen.
            Column(Modifier.weight(1f, fill = false)) {
                if (message != null) {
                    Box(Modifier.heightIn(min = 48.dp)) {
                        Text(
                            message,
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp),
                            style = TagMasterType.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                content?.invoke(this)
            }
            if (confirm != null || dismiss != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End),
                ) {
                    dismiss?.let { DialogTextButton(it) }
                    confirm?.let { DialogTextButton(it) }
                }
            }
        }
    }
}

/**
 * AppCompat's DialogTitle: one line at 24sp, or — when that would not fit — up to two lines at
 * 18sp rather than an ellipsis.
 */
@Composable
private fun DialogTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    val colors = TagMasterTheme.colors
    val measurer = rememberTextMeasurer()
    val large = TagMasterType.headlineSmall.withoutLineHeight()
    val small = TagMasterType.titleLarge.withoutLineHeight().copy(fontSize = 18.sp, letterSpacing = 0.sp).inWholePixels()
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            Text(title, style = large, color = colors.onSurface, maxLines = 1)
            Text(title, style = small, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
    ) { measurables, constraints ->
        val fits = measurer.measure(title, large, maxLines = 1, constraints = Constraints(maxWidth = constraints.maxWidth))
        val chosen = if (fits.hasVisualOverflow) measurables[1] else measurables[0]
        val placeable = chosen.measure(constraints.copy(minWidth = 0))
        layout(constraints.maxWidth, placeable.height) { placeable.place(0, 0) }
    }
}

@Composable
private fun DialogTextButton(button: DialogButton) {
    val colors = TagMasterTheme.colors
    val color =
        when {
            !button.enabled -> colors.onSurface.copy(alpha = 0.38f)
            button.destructive -> colors.error
            else -> colors.primary
        }
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .clickable(enabled = button.enabled, role = Role.Button, onClick = button.onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(button.id),
        contentAlignment = ViewAlign.Center,
    ) {
        Text(button.text, style = TagMasterType.labelLarge.withoutLineHeight(), color = color, maxLines = 1)
    }
}
