package depollsoft.pitchperfect.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced

import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/** A dialog button: engraved caps in ink, as MaterialAlertDialog's text buttons. */
class DialogButton(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val testTag: String? = null,
)

/**
 * The plate's alert dialog, sized and spaced as MaterialAlertDialog (with the plate theme's
 * dialog overlay) laid itself out: a surface card with 2dp corners, [horizontalInset] from the
 * screen edges and at least 95% of a portrait screen wide, a Subtitle1 title, the body and a row
 * of text buttons, positive last.
 */
@Composable
fun PlateAlertDialog(
    onDismissRequest: () -> Unit,
    title: String?,
    buttons: List<DialogButton>,
    neutral: DialogButton? = null,
    verticalInset: Dp = 80.dp,
    horizontalInset: Dp = 24.dp,
    message: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = plateColors
    Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        MatchPlatformWindow(PLATE_DIM)
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenWidth = with(density) { configuration.screenWidthDp.dp.roundToPx() }
        val band = remember { arrayOfNulls<LayoutCoordinates>(2) }
        Box(
            Modifier
                .layout { measurable, constraints ->
                    // The dialog window wraps its content but is at least 95% of a portrait
                    // screen's width (`windowMinWidthMinor`), and never wider than the screen.
                    val minimum = (screenWidth * MIN_WIDTH_FRACTION).toInt()
                    // Wrap-content, as the window was: its content's own width within those bounds.
                    val width =
                        measurable.maxIntrinsicWidth(constraints.maxHeight)
                            .coerceIn(minimum.coerceAtMost(screenWidth), screenWidth)
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
                // MaterialAlertDialog's inset band around the card was part of its window, and a
                // tap there cancelled the dialog as a tap outside the window does.
                .onPlaced { band[0] = it }
                .pointerInput(onDismissRequest) {
                    detectTapGestures { position ->
                        val outer = band[0]
                        val card = band[1]
                        if (outer != null && card != null && !outer.localBoundingBoxOf(card).contains(position)) onDismissRequest()
                    }
                }.padding(horizontal = horizontalInset, vertical = verticalInset)
                // The system resized the old dialog window above the keyboard; this one keeps the
                // card centred in the space the keyboard leaves.
                .windowInsetsPadding(WindowInsets.ime),
        ) {
            Column(
                Modifier
                    .onPlaced { band[1] = it }
                    .dialogEntrance()
                    .clip(DialogShape)
                    .background(colors.surface, DialogShape),
            ) {
                if (title != null) {
                    DialogTitle(title, horizontalInset, Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp))
                }
                if (message != null) {
                    if (title != null) Spacer(Modifier.heightIn(min = 8.dp))
                    PlateText(
                        message,
                        style = plateText(14.sp, colors.ink.copy(alpha = 0.6f), letterSpacing = 0.017857144f),
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = MESSAGE_BOTTOM),
                    )
                }
                content?.invoke(this)
                DialogButtonBar(buttons, neutral)
            }
        }
    }
}

/**
 * AppCompat's alert dialog, which the sign-in prompt and the changelog use: the dialog theme's
 * floating background (not the plate surface), 16dp from the screen edges, a 20sp medium title
 * and its buttons in a bar with the neutral one leading.
 */
@Composable
fun AppCompatAlertDialog(
    onDismissRequest: () -> Unit,
    title: String?,
    buttons: List<DialogButton> = emptyList(),
    neutral: DialogButton? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = plateColors
    val context = LocalView.current.context
    val background =
        androidx.compose.runtime.remember(context) {
            val themed = android.view.ContextThemeWrapper(context, depollsoft.pitchperfect.R.style.AlertDialogTheme)
            themed.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackgroundFloating)).run {
                androidx.compose.ui.graphics.Color(getColor(0, android.graphics.Color.WHITE)).also { recycle() }
            }
        }
    Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        MatchPlatformWindow(APPCOMPAT_DIM)
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenWidth = with(density) { configuration.screenWidthDp.dp.roundToPx() }
        Box(
            Modifier
                .layout { measurable, constraints ->
                    val minimum = (screenWidth * MIN_WIDTH_FRACTION).toInt()
                    // Wrap-content, as the window was: its content's own width within those bounds.
                    val width =
                        measurable.maxIntrinsicWidth(constraints.maxHeight)
                            .coerceIn(minimum.coerceAtMost(screenWidth), screenWidth)
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }.padding(16.dp)
                .windowInsetsPadding(WindowInsets.ime),
        ) {
            Column(Modifier.dialogEntrance().clip(DialogShape).background(background, DialogShape)) {
                if (title != null) {
                    PlateText(
                        title,
                        style = plateText(20.sp, colors.ink, weight = FontWeight.Medium),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp),
                    )
                }
                // The body gets only the height the title and buttons leave, as AlertDialog's
                // scroll panel did, so the buttons stay on screen however tall it is.
                Column(Modifier.weight(1f, fill = false)) { content() }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (neutral != null) PlateTextButton(neutral)
                    Spacer(Modifier.weight(1f))
                    buttons.forEach { PlateTextButton(it) }
                }
            }
        }
    }
}

/** Below a message the scroll panel leaves 38px at xxhdpi before the buttons. */
private val MESSAGE_BOTTOM = (38f / 3f).dp
private const val MIN_WIDTH_FRACTION = 0.95f
private val DialogShape = RoundedCornerShape(2.dp)

@Composable
private fun DialogButtonBar(
    buttons: List<DialogButton>,
    neutral: DialogButton?,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (neutral != null) PlateTextButton(neutral)
        Spacer(Modifier.weight(1f))
        buttons.forEach { PlateTextButton(it) }
    }
}

/** A MaterialComponents text button: 48dp tall with its 6dp insets, 8dp side padding, caps. */
@Composable
fun PlateTextButton(
    button: DialogButton,
    modifier: Modifier = Modifier,
) {
    val colors = plateColors
    Box(
        modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 64.dp)
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(2.dp))
            .clickable(
                enabled = button.enabled,
                role = Role.Button,
                indication = ripple(),
                interactionSource = null,
                onClick = button.onClick,
            ).then(if (button.testTag != null) Modifier.testTag(button.testTag) else Modifier)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
        // The label spans the button, centred as TextView's gravity centred it (at a fractional x).
        propagateMinConstraints = true,
    ) {
        PlateText(
            button.text.uppercase(),
            style =
                plateText(
                    14.sp,
                    if (button.enabled) colors.ink else colors.ink.copy(alpha = 0.38f),
                    weight = FontWeight.Medium,
                    letterSpacing = 0.08928572f,
                ),
            maxLines = 1,
            align = TextAlign.Center,
        )
    }
}

/**
 * Dims the screen behind a dialog as its View-era theme did: MaterialComponents' dialogs by 32%,
 * AppCompat's by 60%. Compose's dialog window would always use the platform's amount. The window
 * also fades out as it closes, however it is closed ([dialogEntrance] brings the card in).
 */
@Composable
private fun MatchPlatformWindow(dim: Float) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    SideEffect {
        window.setDimAmount(dim)
        window.setWindowAnimations(depollsoft.pitchperfect.R.style.Animation_Plate_Dialog)
    }
}

/** A dialog's card fades in from 90% of its size as it opens. */
@Composable
fun Modifier.dialogEntrance(): Modifier {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) { shown.animateTo(1f, tween(DIALOG_ENTER_MS, easing = LinearOutSlowInEasing)) }
    return graphicsLayer {
        alpha = shown.value
        val scale = DIALOG_ENTER_SCALE + (1f - DIALOG_ENTER_SCALE) * shown.value
        scaleX = scale
        scaleY = scale
    }
}

internal const val DIALOG_ENTER_MS = 150
private const val DIALOG_ENTER_SCALE = 0.9f

private const val PLATE_DIM = 0.32f
private const val APPCOMPAT_DIM = 0.6f

/**
 * AppCompat's DialogTitle under the plate dialog theme: one Subtitle1 line, unless the title would
 * ellipsize in the window's first measuring pass, at the platform's preferred dialog width (320dp,
 * less the card's [horizontalInset] and the title's 24dp padding on each side). Then DialogTitle
 * switches to `textAppearanceMedium`'s size, 18sp, over up to two lines, and keeps it however wide
 * the dialog then opens.
 */
@Composable
private fun DialogTitle(
    title: String,
    horizontalInset: Dp,
    modifier: Modifier,
) {
    val colors = plateColors
    val measurer = rememberTextMeasurer()
    val single = plateText(16.sp, colors.ink, letterSpacing = 0.009375f)
    val wrapped = plateText(18.sp, colors.ink, letterSpacing = 0.009375f)
    val density = LocalDensity.current
    val firstPass = with(density) { 320.dp.roundToPx() - 2 * horizontalInset.roundToPx() - 2 * 24.dp.roundToPx() }
    val fits = remember(title, single, firstPass) { !measurer.measure(title, single, maxLines = 1, constraints = Constraints(maxWidth = firstPass)).hasVisualOverflow }
    if (fits) {
        PlateText(title, style = single, maxLines = 1, modifier = modifier)
    } else {
        PlateText(title, style = wrapped, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = modifier)
    }
}
