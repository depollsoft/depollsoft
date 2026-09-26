package depollsoft.pitchperfect.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

/**
 * The plate's outlined text box, as the set list name dialog's TextInputLayout drew it: a 2dp
 * rounded hairline outline with the label floating in a gap in its top edge, the helper or error
 * line in monospace under it, and the outline in ink at 2dp while focused or in error.
 *
 * While the field is empty and unfocused the label rests inside the box at the text's size, and it
 * floats up into the outline as the field is focused or filled; the helper and error lines fade
 * and slide in, all as TextInputLayout animated them.
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
    val density = LocalDensity.current
    var focused by remember { mutableStateOf(false) }
    var fieldHeight by remember { mutableStateOf(0) }
    val strokeColor = if (error != null) colors.ink else colors.hairline
    val strokeWidth = if (focused || error != null) 2.dp else 1.dp
    val floated by animateFloatAsState(
        if (focused || value.text.isNotEmpty()) 1f else 0f,
        tween(LABEL_FLOAT_MS, easing = FastOutSlowInEasing),
        label = "label",
    )
    val floatedStyle = plateText(12.sp, colors.inkSecondary, letterSpacing = 0.033333335f)
    val restingSize = textStyle.fontSize.takeIf { it.isSp } ?: floatedStyle.fontSize
    // The outline's gap is sized to the floated label, whatever size the label is drawn at now.
    val measurer = rememberTextMeasurer()
    val labelWidth =
        remember(label, floatedStyle) {
            // The width PlateText gives the line, as a TextView measured it.
            measurer.measure(label, floatedStyle, maxLines = 1).run { ceil(getLineRight(0) - getLineLeft(0)).toInt() }
        }
    Column(modifier) {
        Box(Modifier.padding(top = LABEL_HALF_HEIGHT)) {
            BasicTextField(
                value,
                onValueChange,
                fieldModifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
                    .onSizeChanged { fieldHeight = it.height }
                    .semantics {
                        contentDescription = label
                        if (error != null) this.error(error)
                    }.drawBehind {
                        val stroke = strokeWidth.toPx()
                        // The label sits in a gap cut from the outline's top edge, opening as it floats up.
                        val startEdge = LABEL_START.toPx() - LABEL_GAP.toPx() * floated
                        val endEdge = LABEL_START.toPx() + (labelWidth + LABEL_GAP.toPx()) * floated
                        val gap = outlineGap(size.width, startEdge, endEdge, layoutDirection)
                        val gapStart = gap.start
                        val gapEnd = gap.endInclusive
                        clipRect(right = gapStart) { outline(strokeColor, stroke) }
                        clipRect(left = gapEnd) { outline(strokeColor, stroke) }
                        clipRect(left = gapStart, right = gapEnd, top = stroke * 2) { outline(strokeColor, stroke) }
                    }.padding(horizontal = 16.dp, vertical = 17.dp),
                textStyle = textStyle,
                singleLine = true,
                cursorBrush = SolidColor(colors.ink),
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        // textCapWords alone: no suggestions or autocorrect for a list's name.
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
            )
            var labelHeight by remember { mutableStateOf(0) }
            PlateText(
                label,
                style = floatedStyle.copy(fontSize = lerp(restingSize, floatedStyle.fontSize, floated)),
                maxLines = 1,
                modifier =
                    Modifier
                        .offset {
                            // Floated, the label straddles the outline's top edge; resting, it is
                            // centred on the text line inside the box.
                            val floatedY = -LABEL_HALF_HEIGHT.roundToPx()
                            val restingY = (fieldHeight - labelHeight) / 2
                            IntOffset(LABEL_START.roundToPx(), (restingY + (floatedY - restingY) * floated).toInt())
                        }.onSizeChanged { labelHeight = it.height },
            )
        }
        val below = error ?: helper
        AnimatedContent(
            targetState = below?.let { it to (error != null) },
            transitionSpec = { captionTransition(with(density) { CAPTION_SLIDE.roundToPx() }) },
            label = "caption",
        ) { caption ->
            if (caption != null) {
                PlateText(
                    caption.first,
                    // The error appearance named monospace, but TextInputLayout never applied its font family.
                    style = plateText(12.sp, if (caption.second) colors.ink else colors.inkSecondary, letterSpacing = 0.033333335f),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
                )
            }
        }
    }
}

/**
 * TextInputLayout's caption change: the new helper or error line fades in over 167ms while sliding
 * down into place, the old one fades out, and the space below the field changes at once.
 */
private fun <S> AnimatedContentTransitionScope<S>.captionTransition(slidePx: Int): ContentTransform =
    (fadeIn(tween(CAPTION_MS)) + slideInVertically(tween(CAPTION_MS)) { -slidePx }) togetherWith
        fadeOut(tween(CAPTION_MS)) using SizeTransform(clip = false) { _, _ -> snap() }

private fun DrawScope.outline(
    color: Color,
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
private const val LABEL_FLOAT_MS = 167
private const val CAPTION_MS = 167
private val CAPTION_SLIDE = 5.dp
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
    fieldModifier: Modifier = Modifier,
    error: String? = null,
    onDone: () -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val colors = plateColors
    var focused by remember { mutableStateOf(false) }
    // TextInputLayout's underline: 1dp of ink at the unfocused stroke's 48% while idle, 2dp of
    // solid ink while focused or in error.
    val active = focused || error != null
    val underline = if (active) 2.dp else 1.dp
    val underlineColor = if (active) colors.ink else colors.ink.copy(alpha = IDLE_UNDERLINE_ALPHA)
    Column(modifier) {
        BasicTextField(
            value,
            onValueChange,
            fieldModifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .semantics {
                    contentDescription = description
                    if (error != null) this.error(error)
                }.drawBehind {
                    // The filled box: the plate surface with the small component's 2dp top corners.
                    val corner = 2.dp.toPx()
                    drawRoundRect(colors.surface, cornerRadius = CornerRadius(corner))
                    drawRect(colors.surface, Offset(0f, corner), Size(size.width, size.height - corner))
                    val height = underline.toPx()
                    drawRect(underlineColor, Offset(0f, size.height - height), Size(size.width, height))
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
        val density = LocalDensity.current
        AnimatedContent(
            targetState = error.orEmpty(),
            transitionSpec = { captionTransition(with(density) { CAPTION_SLIDE.roundToPx() }) },
            label = "error",
        ) { text ->
            PlateText(
                text,
                style = plateText(12.sp, colors.ink, letterSpacing = 0.033333335f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private const val IDLE_UNDERLINE_ALPHA = 0.478f

/**
 * The span cut from the outline's top edge for a label reaching from [startEdge] to [endEdge]
 * after the start edge. The label is placed from the start, so in a right-to-left layout the gap
 * is mirrored across the field's [width].
 */
internal fun outlineGap(
    width: Float,
    startEdge: Float,
    endEdge: Float,
    layoutDirection: LayoutDirection,
): ClosedFloatingPointRange<Float> =
    if (layoutDirection == LayoutDirection.Rtl) (width - endEdge)..(width - startEdge) else startEdge..endEdge
