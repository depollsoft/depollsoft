package depollsoft.pitchperfect.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The plate's floating action button: a 56dp surface disc, a 24dp ink icon, a 6dp lift. */
@Composable
fun PlateFab(
    @DrawableRes icon: Int,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val colors = plateColors
    val interaction = remember { MutableInteractionSource() }
    FabVisibility(visible, modifier, extended = false) {
        Box(
            Modifier
                .size(56.dp)
                .shadow(pressLift(interaction, FAB_ELEVATION), CircleShape)
                .background(elevatedSurface(colors, FAB_ELEVATION_OVERLAY), CircleShape)
                .clip(CircleShape)
                .clickable(role = Role.Button, indication = ripple(), interactionSource = interaction, onClick = onClick)
                .semantics { if (description != null) contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            DrawableIcon(icon, colors.ink)
        }
    }
}

/**
 * The Keys tab's major/minor switch: a hairline-edged surface pill with an icon and an engraved
 * caps label.
 */
@Composable
fun PlateExtendedFab(
    @DrawableRes icon: Int,
    text: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val colors = plateColors
    val shape = RoundedCornerShape(50)
    val interaction = remember { MutableInteractionSource() }
    FabVisibility(visible, modifier, extended = true) {
        Row(
            Modifier
                .heightIn(min = 48.dp)
                .shadow(pressLift(interaction, FAB_ELEVATION), shape)
                .background(elevatedSurface(colors, FAB_ELEVATION_OVERLAY), shape)
                .drawWithContent {
                    drawContent()
                    // MaterialShapeDrawable strokes a path inset by half the stroke width.
                    val stroke = 1.dp.toPx()
                    drawRoundRect(
                        colors.hairline,
                        Offset(stroke / 2f, stroke / 2f),
                        Size(size.width - stroke, size.height - stroke),
                        CornerRadius(size.height / 2f - stroke / 2f),
                        style = Stroke(stroke),
                    )
                }
                .clip(shape)
                .clickable(role = Role.Button, indication = ripple(), interactionSource = interaction, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    role = Role.Button
                }.padding(start = 12.dp, end = 20.dp),
            // An odd leftover pixel goes below the content, as a View's gravity puts it.
            verticalAlignment = Alignment.Vertical { size, space -> (space - size) / 2 },
        ) {
            DrawableIcon(icon, colors.ink)
            Spacer(Modifier.width(12.dp))
            PlateText(
                text.uppercase(),
                style = plateText(14.sp, colors.ink, weight = FontWeight.Medium, letterSpacing = 0.08928572f),
            )
        }
    }
}

/**
 * A FAB's show and hide, as MaterialComponents' motion specs ran them: a FAB fades and scales from
 * nothing and back over 150ms; an extended FAB fades in from 80% of its size over 150ms and just
 * fades out over 75ms.
 */
@Composable
private fun FabVisibility(
    visible: Boolean,
    modifier: Modifier,
    extended: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible,
        modifier,
        enter =
            if (extended) {
                fadeIn(tween(FAB_ANIMATION_MS)) + scaleIn(tween(FAB_ANIMATION_MS), initialScale = EXTENDED_FAB_START_SCALE)
            } else {
                fadeIn(tween(FAB_ANIMATION_MS)) + scaleIn(tween(FAB_ANIMATION_MS))
            },
        exit =
            if (extended) {
                fadeOut(tween(EXTENDED_FAB_HIDE_MS))
            } else {
                fadeOut(tween(FAB_ANIMATION_MS)) + scaleOut(tween(FAB_ANIMATION_MS))
            },
    ) { content() }
}

internal const val FAB_ANIMATION_MS = 150
internal const val EXTENDED_FAB_HIDE_MS = 75
private const val EXTENDED_FAB_START_SCALE = 0.8f
private val FAB_ELEVATION = 6.dp

/**
 * A MaterialComponents button's resting [elevation], lifted as its state list animator lifted it:
 * 6dp more while pressed, 2dp more while focused or hovered, over 100ms. A resting button (no
 * elevation) stays flat.
 */
@Composable
fun pressLift(
    interaction: MutableInteractionSource,
    elevation: Dp,
): Dp {
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val target =
        when {
            elevation == 0.dp -> 0.dp
            pressed -> elevation + PRESSED_LIFT
            focused || hovered -> elevation + FOCUSED_LIFT
            else -> elevation
        }
    val lift by animateDpAsState(target, tween(LIFT_MS), label = "lift")
    return lift
}

private val PRESSED_LIFT = 6.dp
private val FOCUSED_LIFT = 2.dp
private const val LIFT_MS = 100

/**
 * The settings screen's button (`Widget.Plate.SettingsButton`): an outlined surface bar, 36dp
 * visible inside its 48dp touch height, engraved caps in ink.
 */
@Composable
fun PlateSettingsButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = plateColors
    PlateButtonFrame(
        modifier,
        enabled,
        onClick,
        fill = colors.surface,
        stroke = colors.hairline,
    ) {
        PlateText(
            text.uppercase(),
            style = plateText(14.sp, colors.ink.copy(alpha = if (enabled) 1f else 0.38f), PlateFonts.oswald, letterSpacing = 0.12f),
            maxLines = 1,
            align = TextAlign.Center,
        )
    }
}

/** The one primary action on a plate (`Widget.Plate.PrimaryButton`): filled lit ink, engraved caps. */
@Composable
fun PlatePrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = plateColors
    // The style asked for no elevation, but MaterialButton's state animator lifts an enabled
    // filled button by 2dp regardless.
    PlateButtonFrame(
        modifier,
        enabled,
        onClick,
        fill = if (enabled) colors.accent else colors.hairline,
        elevation = if (enabled) 2.dp else 0.dp,
    ) {
        PlateText(
            text.uppercase(),
            style = plateText(14.sp, if (enabled) colors.onAccent else colors.inkSecondary, PlateFonts.oswald, letterSpacing = 0.12f),
            maxLines = 1,
            align = TextAlign.Center,
        )
    }
}

/**
 * A MaterialComponents contained button in the plate theme: surface fill, 2dp lift, ink label in
 * the theme's button type, not upper-cased.
 */
@Composable
fun PlateContainedButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = plateColors
    PlateButtonFrame(modifier, true, onClick, fill = elevatedSurface(colors, BUTTON_ELEVATION_OVERLAY), elevation = 2.dp) {
        PlateText(
            text,
            style = plateText(14.sp, colors.ink, weight = FontWeight.Medium, letterSpacing = 0.08928572f),
            maxLines = 1,
            align = TextAlign.Center,
        )
    }
}

/** The shared MaterialButton frame: 6dp insets above and below, 2dp corners, 16dp side padding. */
@Composable
private fun PlateButtonFrame(
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
    fill: Color,
    stroke: Color? = null,
    elevation: Dp = 0.dp,
    label: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(2.dp)
    val interaction = remember { MutableInteractionSource() }
    val lifted = pressLift(interaction, elevation)
    Box(
        modifier
            .heightIn(min = 48.dp)
            .padding(vertical = 6.dp)
            .then(if (elevation > 0.dp) Modifier.shadow(lifted, shape) else Modifier)
            .background(fill, shape)
            .then(if (stroke != null) Modifier.border(1.dp, stroke, shape) else Modifier)
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, indication = ripple(), interactionSource = interaction, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
        // The label spans the button, centred as TextView's gravity centred it (at a fractional x).
        propagateMinConstraints = true,
    ) { label() }
}

/**
 * The plate surface under an elevated control. In the dark theme MaterialComponents lightens an
 * elevated surface with a white overlay of [overlay].
 */
private fun elevatedSurface(
    colors: PlateColors,
    overlay: Float,
): Color = if (colors.surface.luminance() < 0.5f) Color.White.copy(alpha = overlay).compositeOver(colors.surface) else colors.surface

/** A 2dp button's overlay, and a 6dp floating action button's, as measured in the dark theme. */
private const val BUTTON_ELEVATION_OVERLAY = 0.056f
private const val FAB_ELEVATION_OVERLAY = 0.086f
