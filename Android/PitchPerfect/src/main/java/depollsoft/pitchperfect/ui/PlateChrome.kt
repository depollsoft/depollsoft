package depollsoft.pitchperfect.ui

import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.annotation.DrawableRes
import androidx.appcompat.R as AppCompatR
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One device pixel, for the plate's hairlines: the View drawables drew them as `1px`. */
val hairlineWidth: Dp
    @Composable get() = with(LocalDensity.current) { 1.toDp() }

/** Whether the tablet (`sw600dp`) layouts apply. */
val isTablet: Boolean
    @Composable get() = LocalConfiguration.current.smallestScreenWidthDp >= 600

/** A one-pixel hairline along the bottom edge, like `bar_surface_bottom_edge`. */
fun Modifier.bottomHairline(color: Color): Modifier =
    drawBehind { drawRect(color, Offset(0f, size.height - 1f), Size(size.width, 1f)) }

/** A one-pixel hairline along the top edge. */
fun Modifier.topHairline(color: Color): Modifier = drawBehind { drawRect(color, Offset.Zero, Size(size.width, 1f)) }

/** A one-pixel hairline along the trailing edge, like `bar_surface_end_edge`. */
fun Modifier.endHairline(color: Color): Modifier =
    drawBehind { drawRect(color, Offset(size.width - 1f, 0f), Size(1f, size.height)) }

/**
 * Centres the content vertically the way a View's gravity does: an odd leftover pixel goes below,
 * where Compose's alignment would round it above.
 */
fun Modifier.centerVerticallyLikeViews(): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minHeight = 0))
        val height = constraints.maxHeight
        layout(placeable.width, height) { placeable.placeRelative(0, (height - placeable.height) / 2) }
    }

/** The theme's `actionBarSize`, the height the window action bar had. */
@Composable
fun actionBarHeight(): Dp {
    val context = LocalContext.current
    val density = LocalDensity.current
    val pixels =
        remember(context) {
            val value = TypedValue()
            context.theme.resolveAttribute(AppCompatR.attr.actionBarSize, value, true)
            TypedValue.complexToDimensionPixelSize(value.data, context.resources.displayMetrics)
        }
    return with(density) { pixels.toDp() }
}

/**
 * The action bar: the plate's surface with a hairline under it, the engraved title and the
 * screen's actions, as the window action bar drew them.
 */
@Composable
fun PlateTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationUp: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = plateColors
    // The tablet toolbar keeps 8dp inside each edge; the phone's actions run to the edge.
    val sidePadding = if (isTablet) 8.dp else 0.dp
    Row(
        modifier
            .fillMaxWidth()
            .height(actionBarHeight())
            .background(colors.surface)
            .bottomHairline(colors.hairline)
            .padding(horizontal = sidePadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigationUp != null) {
            UpButton(navigationUp)
        }
        Box(
            Modifier
                .weight(1f)
                .padding(start = 16.dp)
                .centerVerticallyLikeViews(),
        ) {
            PlateText(
                title,
                // The window action bar kept its own title face (Roboto Medium); the theme's
                // Oswald family never reached it, so neither does it here.
                style = plateText(19.sp, colors.ink, weight = FontWeight.Medium, letterSpacing = 0.16f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        actions()
    }
}

/** The toolbar's "Navigate up" button: a 56dp square around the back arrow. */
@Composable
private fun UpButton(onClick: () -> Unit) {
    val colors = plateColors
    val description = stringResource(AppCompatR.string.abc_action_bar_up_description)
    WithTooltip(description) { showTooltip ->
        Box(
            Modifier
                .size(56.dp)
                .combinedClickable(
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 28.dp),
                    onLongClick = showTooltip,
                    onClick = onClick,
                ).semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            DrawableIcon(AppCompatR.drawable.abc_ic_ab_back_material, colors.ink)
        }
    }
}

/** An action bar icon: a 48dp target around a 24dp icon in ink. */
@Composable
fun PlateActionIcon(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = plateColors
    // An icon-only action names itself in a tooltip, as the action bar's did.
    WithTooltip(description) { showTooltip ->
        Box(
            modifier
                .size(48.dp)
                .combinedClickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 20.dp),
                    onLongClick = showTooltip,
                    onClick = onClick,
                ).semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            DrawableIcon(icon, colors.ink, alpha = if (enabled) 1f else 0.38f)
        }
    }
}

/** A navigation destination: its label, its icon and the tag tests find it by. */
class PlateDestination(
    val label: String,
    @param:DrawableRes val icon: Int,
    val testTag: String,
)

/**
 * The phone's bottom navigation bar: equal items, each an icon over its label, the current one in
 * full ink with a bold label.
 */
@Composable
fun PlateBottomNavigation(
    destinations: List<PlateDestination>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = plateColors
    Layout(
        content = {
            destinations.forEachIndexed { index, destination ->
                NavigationItem(destination, index, index == selected, rail = false) { onSelect(index) }
            }
        },
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(colors.surface)
                .topHairline(colors.hairline)
                // A screen reader hears the bar as tabs: "Tab 2 of 4".
                .semantics { collectionInfo = CollectionInfo(rowCount = 1, columnCount = destinations.size) },
    ) { measurables, constraints ->
        // BottomNavigationMenuView's split: equal widths, the leftover pixels to the first items.
        val width = constraints.maxWidth
        val base = width / measurables.size
        val extra = width - base * measurables.size
        val height = constraints.maxHeight
        val placeables =
            measurables.mapIndexed { index, measurable ->
                measurable.measure(Constraints.fixed(base + if (index < extra) 1 else 0, height))
            }
        layout(width, height) {
            var x = 0
            placeables.forEach {
                it.placeRelative(x, 0)
                x += it.width
            }
        }
    }
}

/** The tablet's navigation rail: the same destinations stacked down the leading edge. */
@Composable
fun PlateNavigationRail(
    destinations: List<PlateDestination>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = plateColors
    Layout(
        content = {
            destinations.forEachIndexed { index, destination ->
                NavigationItem(destination, index, index == selected, rail = true) { onSelect(index) }
            }
        },
        modifier =
            modifier
                .background(colors.surface)
                .endHairline(colors.hairline)
                .semantics { collectionInfo = CollectionInfo(rowCount = destinations.size, columnCount = 1) },
    ) { measurables, constraints ->
        val item = RAIL_ITEM.roundToPx()
        val top = RAIL_TOP.roundToPx()
        val placeables = measurables.map { it.measure(Constraints.fixed(item, item)) }
        layout(item, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable -> placeable.placeRelative(0, top + index * item) }
        }
    }
}

private val RAIL_ITEM = 72.dp
private val RAIL_TOP = 8.dp

/**
 * One destination. Both a regular and a bold copy of the label are laid out and one is shown, so
 * the item is as wide as the wider of the two whichever is selected.
 */
@Composable
private fun NavigationItem(
    destination: PlateDestination,
    index: Int,
    selected: Boolean,
    rail: Boolean,
    onClick: () -> Unit,
) {
    val colors = plateColors
    val tint = if (selected) colors.ink else colors.inkSecondary
    @Suppress("DEPRECATION")
    val regular =
        plateText(12.sp, tint, letterSpacing = 0.033333335f)
            .copy(platformStyle = PlatformTextStyle(includeFontPadding = rail))
    val labelPadding = if (rail) 2.dp else 0.dp
    WithTooltip(destination.label) { showTooltip ->
        Layout(
            content = {
                DrawableIcon(destination.icon, tint)
                PlateText(
                    destination.label,
                    style = regular.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    align = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = labelPadding).alpha(if (selected) 1f else 0f),
                )
                PlateText(
                    destination.label,
                    style = regular,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    align = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = labelPadding).alpha(if (selected) 0f else 1f),
                )
            },
            modifier =
                Modifier
                    .testTag(destination.testTag)
                    // As MaterialComponents' items: a tab in a collection, and the current one offers
                    // no click to a screen reader (a tap on it still does nothing but ripple).
                    .clearAndSetSemantics {
                        contentDescription = destination.label
                        role = Role.Tab
                        this.selected = selected
                        collectionItemInfo =
                            if (rail) CollectionItemInfo(index, 1, 0, 1) else CollectionItemInfo(0, 1, index, 1)
                        if (!selected) {
                            onClick {
                                onClick()
                                true
                            }
                        }
                        onLongClick {
                            showTooltip()
                            true
                        }
                    }.tabGestures(onClick, showTooltip),
        ) { measurables, constraints ->
            val icon = measurables[0].measure(Constraints())
            // A label wider than its item (large text) is cut short with an ellipsis, as
            // MaterialComponents' single-line labels were, rather than running into its neighbours.
            val label = Constraints(maxWidth = constraints.maxWidth)
            val bold = measurables[1].measure(label)
            val plain = measurables[2].measure(label)
            val content = maxOf(icon.width, bold.width, plain.width)
            val width = constraints.maxWidth
            val left = (width - content) / 2
            val iconTop = (if (rail) RAIL_ICON_TOP else BAR_ICON_TOP).toPx().toInt()
            val labelTop = (if (rail) RAIL_LABEL_TOP else BAR_LABEL_TOP).toPx().toInt()
            layout(width, constraints.maxHeight) {
                icon.placeRelative(left + (content - icon.width) / 2, iconTop)
                bold.placeRelative(left + (content - bold.width) / 2, labelTop)
                plain.placeRelative(left + (content - plain.width) / 2, labelTop)
            }
        }
    }
}

/**
 * A navigation item's taps, long press, keyboard activation and ripple, with no semantics of their
 * own, so the item's semantics can leave the click out while it is the current tab.
 */
@Composable
private fun Modifier.tabGestures(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    val click by rememberUpdatedState(onClick)
    val longClick by rememberUpdatedState(onLongClick)
    return indication(interaction, ripple())
        .pointerInput(interaction) {
            detectTapGestures(
                onPress = { position ->
                    val press = PressInteraction.Press(position)
                    interaction.emit(press)
                    interaction.emit(if (tryAwaitRelease()) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                },
                onLongPress = {
                    // combinedClickable would give the long press its buzz; this gesture gives its own.
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    longClick()
                },
                onTap = {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    click()
                },
            )
        }.focusable(interactionSource = interaction)
        .onKeyEvent { event ->
            val activates = event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter
            if (activates && event.type == KeyEventType.KeyUp) {
                click()
                true
            } else {
                activates
            }
        }
}

// Where MaterialComponents' navigation views put the icon and the label within an item.
private val BAR_ICON_TOP = 8.dp
private val BAR_LABEL_TOP = 33.dp
private val RAIL_ICON_TOP = 21.5.dp
private val RAIL_LABEL_TOP = 49.5.dp
