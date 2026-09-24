package depollsoft.pitchperfect.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The plate's outlined text box, as the set list name dialog's TextInputLayout drew it: a 2dp
 * rounded hairline outline with the label floating in a gap in its top edge, the helper or error
 * line in monospace under it, and the outline in ink at 2dp while focused or in error.
 */
@Composable
fun PlateOutlinedField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    helper: String? = null,
    error: String? = null,
    onDone: () -> Unit = {},
) {
    val colors = plateColors
    var focused by remember { mutableStateOf(false) }
    var labelWidth by remember { mutableStateOf(0) }
    val strokeColor = if (error != null) colors.ink else colors.hairline
    val strokeWidth = if (focused || error != null) 2.dp else 1.dp
    Column(modifier) {
        Box(Modifier.padding(top = LABEL_HALF_HEIGHT)) {
            BasicTextField(
                value,
                onValueChange,
                fieldModifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
                    .semantics {
                        contentDescription = label
                        if (error != null) this.error(error)
                    }.drawBehind {
                        val stroke = strokeWidth.toPx()
                        val gapStart = LABEL_START.toPx() - LABEL_GAP.toPx()
                        val gapEnd = LABEL_START.toPx() + labelWidth + LABEL_GAP.toPx()
                        // The label sits in a gap cut from the outline's top edge.
                        clipRect(right = gapStart) { outline(strokeColor, stroke) }
                        clipRect(left = gapEnd) { outline(strokeColor, stroke) }
                        clipRect(left = gapStart, right = gapEnd, top = stroke * 2) { outline(strokeColor, stroke) }
                    }.padding(horizontal = 16.dp, vertical = 17.dp),
                textStyle = textStyle,
                singleLine = true,
                cursorBrush = SolidColor(colors.ink),
                keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
            )
            PlateText(
                label,
                style = plateText(12.sp, colors.inkSecondary, letterSpacing = 0.033333335f),
                modifier =
                    Modifier
                        .offset(x = LABEL_START, y = -LABEL_HALF_HEIGHT)
                        .onGloballyPositioned { labelWidth = it.size.width },
            )
        }
        val below = error ?: helper
        if (below != null) {
            PlateText(
                below,
                style = plateText(12.sp, if (error != null) colors.ink else colors.inkSecondary, PlateFonts.mono),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.outline(
    color: androidx.compose.ui.graphics.Color,
    stroke: Float,
) {
    drawRoundRect(
        color,
        Offset(stroke / 2f, stroke / 2f),
        Size(size.width - stroke, size.height - stroke),
        CornerRadius(2.dp.toPx()),
        style = Stroke(stroke),
    )
}

private val LABEL_HALF_HEIGHT = 5.33.dp
private val LABEL_START = 16.dp
private val LABEL_GAP = 4.dp

/**
 * The song title field (a filled TextInputLayout with a clear box): the text over an ink
 * underline that thickens while focused, the hint in secondary ink, and a line kept free below it
 * for the error so the layout never jumps.
 */
@Composable
fun PlateFilledField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    hint: String,
    description: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    error: String? = null,
    onDone: () -> Unit = {},
    focusRequester: androidx.compose.ui.focus.FocusRequester = remember { androidx.compose.ui.focus.FocusRequester() },
) {
    val colors = plateColors
    var focused by remember { mutableStateOf(false) }
    val underline = if (focused) 2.dp else 1.dp
    Column(modifier) {
        BasicTextField(
            value,
            onValueChange,
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .semantics {
                    contentDescription = description
                    if (error != null) this.error(error)
                }.drawBehind {
                    val height = underline.toPx()
                    drawRect(colors.ink, Offset(0f, size.height - height), Size(size.width, height))
                }.padding(vertical = 10.dp),
            textStyle = textStyle,
            singleLine = true,
            cursorBrush = SolidColor(colors.ink),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    autoCorrectEnabled = true,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox = { field ->
                Box {
                    if (value.text.isEmpty()) PlateText(hint, style = textStyle.copy(color = colors.inkSecondary))
                    field()
                }
            },
        )
        PlateText(
            error.orEmpty(),
            style = plateText(12.sp, colors.ink, PlateFonts.mono, letterSpacing = 0.033333335f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
