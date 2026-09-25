package depollsoft.tagmaster.ui

import depollsoft.compose.ListMotion
import depollsoft.compose.PlatformIcon
import depollsoft.compose.ViewAlign
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import depollsoft.tagmaster.ui.TagMasterType.withoutLineHeight
import kotlin.math.roundToInt

/** The trailing or leading icon of an [OutlinedField]: a 48dp touch target around a 24dp icon. */
class FieldIcon(
    val drawable: Int,
    val description: String? = null,
    /** Degrees the icon is turned: the dropdown arrow points up while its list is open. */
    val rotation: Float = 0f,
    val onClick: (() -> Unit)? = null,
)

/**
 * A Material 3 outlined text box, laid out as TextInputLayout lays one out: the label floats in a
 * notch cut from the 1dp outline (or sits inside as a hint while the empty field is unfocused), the
 * text is inset 16dp, and optional icons sit in 48dp slots; helper, error and counter text follow.
 *
 * [value]/[onValueChange] make it editable; without [onValueChange] it shows [value] read-only
 * (the exposed dropdowns).
 */
@Composable
fun OutlinedField(
    label: String,
    value: TextFieldValue,
    modifier: Modifier = Modifier,
    onValueChange: ((TextFieldValue) -> Unit)? = null,
    textStyle: TextStyle = TagMasterType.bodyLarge.withoutLineHeight(),
    textColor: Color = TagMasterTheme.colors.onSurface,
    startIcon: FieldIcon? = null,
    endIcon: FieldIcon? = null,
    helper: String? = null,
    error: String? = null,
    /** Characters entered and the limit; past the limit the counter and outline turn to the error color. */
    counter: Pair<Int, Int>? = null,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    fieldModifier: Modifier = Modifier,
    /** Drawn as focused: an exposed dropdown whose list is open. */
    active: Boolean = false,
    /** Whether [endIcon] shows; it fades and scales in and out as TextInputLayout's clear icon did. */
    endIconVisible: Boolean = true,
) {
    val colors = TagMasterTheme.colors
    val density = LocalDensity.current
    val hasFocus by interactionSource.collectIsFocusedAsState()
    val focused = hasFocus || active
    val collapsed = focused || value.text.isNotEmpty() || onValueChange == null
    val progress by animateFloatAsState(if (collapsed) 1f else 0f, tween(167), label = "label")
    // TextInputLayout draws its floating label at a float size, not whole pixels as a TextView does.
    val labelStyle = TagMasterType.bodySmall.withoutLineHeight().copy(fontSize = 12.sp, platformStyle = PlatformTextStyle(includeFontPadding = false))
    val measurer = rememberTextMeasurer()
    val labelLayout = remember(label, labelStyle) { measurer.measure(label, labelStyle) }
    val labelAscent = with(density) { labelLayout.firstBaseline.roundToInt() }
    val topMargin = labelAscent / 2
    val overflowed = counter != null && counter.first > counter.second
    val stroke =
        when {
            error != null || overflowed -> colors.error
            focused -> colors.primary
            else -> colors.outline
        }
    val strokeWidth = if (focused || error != null || overflowed) 2.dp else 1.dp
    val labelColor =
        when {
            error != null || overflowed -> colors.error
            focused -> colors.primary
            else -> colors.onSurfaceVariant
        }
    val hintStyle = textStyle.copy(platformStyle = PlatformTextStyle(includeFontPadding = true))

    Column(modifier.fillMaxWidth()) {
        Layout(
            content = {
                Box(
                    Modifier
                        .layoutId("box")
                        .drawBehind {
                            val inset = strokeWidth.toPx() / 2
                            val labelLeft = 16.dp.toPx()
                            val notchStart = labelLeft - 4.dp.toPx()
                            val notchEnd = labelLeft + labelLayout.size.width + 4.dp.toPx()
                            val drawOutline = {
                                drawRoundRect(
                                    stroke,
                                    topLeft = Offset(inset, inset),
                                    size = Size(size.width - inset * 2, size.height - inset * 2),
                                    cornerRadius = CornerRadius(4.dp.toPx()),
                                    style = Stroke(strokeWidth.toPx()),
                                )
                            }
                            if (progress > 0f && label.isNotEmpty()) {
                                val gap = (notchEnd - notchStart) * progress
                                clipRect(right = notchStart) { drawOutline() }
                                clipRect(left = notchStart + gap) { drawOutline() }
                                clipRect(left = notchStart, right = notchStart + gap, top = strokeWidth.toPx() * 2) { drawOutline() }
                            } else {
                                drawOutline()
                            }
                        },
                ) {
                    Row(verticalAlignment = ViewAlign.CenterVertically) {
                        if (startIcon != null) {
                            FieldIconSlot(startIcon, colors.onSurfaceVariant, Modifier.padding(end = 4.dp))
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(
                                    start = if (startIcon != null) 0.dp else 16.dp,
                                    end = if (endIcon != null && endIconVisible) 0.dp else 16.dp,
                                    top = 17.dp,
                                    bottom = 17.dp,
                                ),
                        ) {
                            if (onValueChange != null) {
                                BasicTextField(
                                    value = value,
                                    onValueChange = onValueChange,
                                    // The field is named by its label, as TextInputLayout named its EditText.
                                    modifier =
                                        fieldModifier
                                            .fillMaxWidth()
                                            .semantics { if (label.isNotEmpty()) contentDescription = label },
                                    textStyle = hintStyle.copy(color = textColor),
                                    maxLines = maxLines,
                                    singleLine = maxLines == 1,
                                    keyboardOptions = keyboardOptions,
                                    keyboardActions = keyboardActions,
                                    interactionSource = interactionSource,
                                    cursorBrush = SolidColor(colors.primary),
                                )
                                if (progress < 1f && value.text.isEmpty()) {
                                    Text(
                                        label,
                                        Modifier.clearAndSetSemantics { },
                                        style = hintStyle,
                                        color = colors.onSurfaceVariant.copy(alpha = 1f - progress),
                                    )
                                }
                            } else {
                                Text(value.text, fieldModifier, style = hintStyle, color = textColor, maxLines = maxLines)
                            }
                        }
                        if (endIcon != null) {
                            AnimatedVisibility(
                                endIconVisible,
                                enter = fadeIn(tween(100)) + scaleIn(tween(150), initialScale = 0.8f),
                                exit = fadeOut(tween(100)) + scaleOut(tween(150), targetScale = 0.8f),
                            ) {
                                FieldIconSlot(endIcon, colors.onSurfaceVariant, Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }
                if (label.isNotEmpty()) {
                    Text(
                        label,
                        Modifier
                            .layoutId("label")
                            .clearAndSetSemantics { },
                        style = labelStyle,
                        color = labelColor.copy(alpha = progress),
                    )
                }
            },
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val box = measurables.first { it.layoutId == "box" }.measure(Constraints.fixedWidth(width))
            val labelPlaceable = measurables.firstOrNull { it.layoutId == "label" }?.measure(Constraints())
            val height = topMargin + box.height
            layout(width, height) {
                box.placeRelative(0, topMargin)
                labelPlaceable?.placeRelative(16.dp.roundToPx(), 0)
            }
        }
        if (helper != null || error != null || counter != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp),
            ) {
                Text(
                    error ?: helper ?: "",
                    Modifier.weight(1f),
                    style = TagMasterType.bodySmall.withoutLineHeight(),
                    color = if (error != null) colors.error else colors.onSurfaceVariant,
                )
                if (counter != null) {
                    val (count, limit) = counter
                    val spoken =
                        stringResource(
                            if (overflowed) {
                                com.google.android.material.R.string.character_counter_overflowed_content_description
                            } else {
                                com.google.android.material.R.string.character_counter_content_description
                            },
                            count,
                            limit,
                        )
                    Text(
                        "$count/$limit",
                        Modifier
                            .padding(start = 16.dp)
                            .semantics { contentDescription = spoken },
                        style = TagMasterType.bodySmall.withoutLineHeight(),
                        color = if (overflowed) colors.error else colors.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldIconSlot(
    icon: FieldIcon,
    tint: Color,
    modifier: Modifier,
) {
    Box(
        modifier
            .size(48.dp)
            .then(
                if (icon.onClick != null) {
                    Modifier
                        .clickable(role = Role.Button, onClick = icon.onClick)
                        .semantics { icon.description?.let { contentDescription = it } }
                } else {
                    Modifier
                },
            ),
        contentAlignment = ViewAlign.Center,
    ) {
        PlatformIcon(icon.drawable, Modifier.rotate(icon.rotation), tint = tint)
    }
}

/**
 * An exposed dropdown: a read-only outlined field showing the chosen [choices] entry, opening the
 * list of choices below itself when tapped.
 */
@Composable
fun DropdownField(
    label: String,
    choices: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "dropdown:$label",
) {
    val colors = TagMasterTheme.colors
    val density = LocalDensity.current
    var expanded by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableIntStateOf(0) }
    // While the list is open the field shows as focused and its arrow turns up, as
    // MaterialAutoCompleteTextView's did.
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, tween(ListMotion.CHANGE_MILLIS), label = "arrow")
    Box(modifier.fillMaxWidth()) {
        OutlinedField(
            label = label,
            value = TextFieldValue(choices.getOrElse(selected.coerceAtLeast(0)) { "" }),
            textStyle = TagMasterType.bodyMedium.withoutLineHeight(),
            textColor = colors.text,
            endIcon = FieldIcon(com.google.android.material.R.drawable.mtrl_dropdown_arrow, rotation = arrow),
            active = expanded,
            modifier =
                Modifier
                    .onSizeChanged { fieldWidth = it.width }
                    .clickable(role = Role.DropdownList) { expanded = true }
                    .semantics { role = Role.DropdownList }
                    .testTag(tag),
        )
        // The list is as wide as the field and marks the current choice.
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(density) { fieldWidth.toDp() }),
            containerColor = colors.surfaceContainerHigh,
        ) {
            choices.forEachIndexed { index, choice ->
                val isSelected = index == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            choice,
                            style = TagMasterType.bodyLarge.withoutLineHeight(),
                            color = if (isSelected) colors.onSecondaryContainer else colors.onSurface,
                        )
                    },
                    modifier =
                        Modifier
                            .then(if (isSelected) Modifier.background(colors.secondaryContainer) else Modifier)
                            .semantics { this.selected = isSelected }
                            .testTag("$tag:$index"),
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                )
            }
        }
    }
}
