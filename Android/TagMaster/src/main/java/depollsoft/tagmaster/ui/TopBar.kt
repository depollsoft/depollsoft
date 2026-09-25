package depollsoft.tagmaster.ui

import depollsoft.compose.PlatformIcon
import depollsoft.compose.ViewAlign
import depollsoft.compose.OpensOnMenuKey
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import depollsoft.tagmaster.R
import depollsoft.tagmaster.ui.TagMasterType.withoutLineHeight

/** Where an action goes, as a menu item's `showAsAction` says. */
enum class ShowAs { Always, IfRoom, Never }

/** One toolbar action: an icon (or, without one, its title as text) or an overflow menu entry. */
data class BarAction(
    val id: String,
    val title: String,
    @param:DrawableRes val icon: Int? = null,
    val showAs: ShowAs = ShowAs.IfRoom,
    val enabled: Boolean = true,
    /** Dims the icon of a disabled step action the way the list pane did (alpha 97/255). */
    val iconAlpha: Float = 1f,
    val onClick: () -> Unit,
)

private const val TITLE_FONT = "fonts/wickhop-handwriting.ttf"

/** The Wickhop display face the app name is set in, capped so its full bounds fit the bar. */
internal class BrandTitle(
    context: Context,
) {
    val family = FontFamily(Font(TITLE_FONT, context.assets))
    private val typeface = Typeface.createFromAsset(context.assets, TITLE_FONT)

    /**
     * The size, in pixels, the title is drawn at: 22sp as the toolbar's TextView read it (through
     * the platform's non-linear font scaling, rounded as `getDimensionPixelSize` rounds), shrunk
     * until the font is 40dp tall.
     */
    fun sizePx(context: Context): Float {
        val metrics = context.resources.displayMetrics
        val sp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 22f, metrics)
        val paint = Paint().apply {
            typeface = this@BrandTitle.typeface
            textSize = (sp + 0.5f).toInt().coerceAtLeast(1).toFloat()
        }
        val font = paint.fontMetrics
        val maxHeight = 40f * metrics.density
        val height = font.bottom - font.top
        return if (height > maxHeight) paint.textSize * maxHeight / height else paint.textSize
    }
}

/**
 * How many action buttons a bar of this configuration shows before the overflow menu, exactly as
 * AppCompat's `ActionBarPolicy` decides.
 */
fun maxActionButtons(
    widthDp: Int,
    heightDp: Int,
    smallestDp: Int,
): Int =
    when {
        smallestDp > 600 || widthDp > 600 || (widthDp > 960 && heightDp > 720) || (widthDp > 720 && heightDp > 960) -> 5
        widthDp >= 500 || (widthDp > 640 && heightDp > 480) || (widthDp > 480 && heightDp > 640) -> 4
        widthDp >= 360 -> 3
        else -> 2
    }

/** Splits [actions] into the ones shown as buttons and the ones in the overflow menu. */
fun splitActions(
    actions: List<BarAction>,
    maxButtons: Int,
): Pair<List<BarAction>, List<BarAction>> {
    val required = actions.count { it.showAs == ShowAs.Always }
    val requested = actions.count { it.showAs == ShowAs.IfRoom }
    val hasOverflow = actions.any { it.showAs == ShowAs.Never }
    var slots = maxButtons
    if (hasOverflow || required + requested > slots) slots--
    slots -= required
    val shown = mutableListOf<BarAction>()
    val overflow = mutableListOf<BarAction>()
    for (action in actions) {
        when (action.showAs) {
            ShowAs.Always -> shown += action
            ShowAs.IfRoom ->
                if (slots > 0) {
                    shown += action
                    slots--
                } else {
                    overflow += action
                }
            ShowAs.Never -> overflow += action
        }
    }
    return shown to overflow
}

/**
 * The charcoal top app bar every screen carries: an optional Up button, the title, action
 * buttons and the overflow menu, placed where MaterialToolbar placed them.
 */
@Composable
fun TagMasterTopBar(
    title: String,
    modifier: Modifier = Modifier,
    brandTitle: Boolean = false,
    onNavigateUp: (() -> Unit)? = null,
    actions: List<BarAction> = emptyList(),
    titleIsHeading: Boolean = false,
    paneTitle: String? = null,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val colors = TagMasterTheme.colors
    val (shown, overflow) =
        splitActions(
            actions,
            maxActionButtons(configuration.screenWidthDp, configuration.screenHeightDp, configuration.smallestScreenWidthDp),
        )
    val brand = remember(context) { BrandTitle(context) }
    val titleStyle =
        if (brandTitle) {
            val size = with(density) { brand.sizePx(context).toSp() }
            TagMasterType.titleLarge.withoutLineHeight().copy(fontFamily = brand.family, fontSize = size)
        } else {
            TagMasterType.titleLarge.withoutLineHeight()
        }
    var menuOpen by remember { mutableStateOf(false) }
    // A screen's own bar names its window, as the activity title bound to the toolbar did.
    val activity = context as? android.app.Activity
    if (paneTitle == null && activity != null) SideEffect { activity.title = title }
    val navIcon = remember(context) { themeDrawableRes(context, androidx.appcompat.R.attr.homeAsUpIndicator) }
    val overflowIcon = remember(context) { overflowDrawableRes(context) }

    if (overflow.isNotEmpty()) OpensOnMenuKey { menuOpen = true }
    OnChrome {
        Layout(
            modifier =
                modifier
                    .fillMaxWidth()
                    .background(colors.chrome)
                    // The toolbar's title was the window's pane title, so TalkBack announces a new
                // screen or a renamed list by its title; beside another pane, the pane's own.
                .semantics { this.paneTitle = paneTitle ?: title },
            content = {
                if (onNavigateUp != null) {
                    val up = context.getString(androidx.appcompat.R.string.abc_action_bar_up_description)
                    WithTooltip(up, Modifier.layoutId("nav")) { tooltip ->
                        Box(
                            Modifier
                                .size(56.dp)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 20.dp),
                                    role = Role.Button,
                                    onLongClick = tooltip::longPressed,
                                    onClick = onNavigateUp,
                                ).semantics { contentDescription = up }
                                .testTag("navigateUp"),
                            contentAlignment = ViewAlign.Center,
                        ) {
                            PlatformIcon(navIcon, tint = colors.onChrome)
                        }
                    }
                }
                Text(
                    title,
                    modifier =
                        Modifier
                            .layoutId("title")
                            .testTag("toolbarTitle")
                            // A TextView clips glyph overhang at its bounds; the Wickhop "T" has some.
                            .clipToBounds()
                            .semantics { if (titleIsHeading) heading() },
                    style = titleStyle,
                    color = colors.onChrome,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                for (action in shown) {
                    ActionButton(action, colors, Modifier.layoutId("action"))
                }
                if (overflow.isNotEmpty()) {
                    Box(Modifier.layoutId("overflow")) {
                        val more = context.getString(androidx.appcompat.R.string.abc_action_menu_overflow_description)
                        WithTooltip(more) { tooltip ->
                            Box(
                                Modifier
                                    .size(40.dp, 48.dp)
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = false, radius = 20.dp),
                                        role = Role.Button,
                                        onLongClick = tooltip::longPressed,
                                    ) { menuOpen = true }
                                    .semantics { contentDescription = more }
                                    .testTag("overflowMenu")
                                    .padding(start = 6.dp, end = 10.dp),
                                contentAlignment = ViewAlign.Center,
                            ) {
                                PlatformIcon(overflowIcon, tint = colors.onChrome)
                            }
                        }
                        OverflowMenu(menuOpen, overflow) { menuOpen = false }
                    }
                }
            },
        ) { measurables, constraints ->
            // MaterialToolbar on a large screen pads its sides by 8dp; its title keeps a 12dp content
            // inset plus a 4dp title margin, measured from the Up button when there is one.
            val height = 64.dp.roundToPx()
            val width = constraints.maxWidth
            val sidePadding = if (configuration.smallestScreenWidthDp >= 600) 8.dp.roundToPx() else 0
            val nav = measurables.firstOrNull { it.layoutId == "nav" }?.measure(Constraints())
            val buttons = measurables.filter { it.layoutId == "action" || it.layoutId == "overflow" }.map { it.measure(Constraints()) }
            val menuWidth = buttons.sumOf { it.width }
            val titleLeft = maxOf(sidePadding + (nav?.width ?: 0), 12.dp.roundToPx()) + 4.dp.roundToPx()
            val menuLeft = width - sidePadding - menuWidth
            val titleRoom = (menuLeft - titleLeft - 4.dp.roundToPx()).coerceAtLeast(0)
            val titlePlaceable = measurables.first { it.layoutId == "title" }.measure(Constraints(maxWidth = titleRoom))
            layout(width, height) {
                nav?.placeRelative(sidePadding, (height - nav.height) / 2)
                titlePlaceable.placeRelative(titleLeft, (height - titlePlaceable.height) / 2)
                var x = menuLeft
                for (button in buttons) {
                    button.placeRelative(x, (height - button.height) / 2)
                    x += button.width
                }
            }
    }
    }
}

@Composable
private fun ActionButton(
    action: BarAction,
    colors: TagMasterColors,
    modifier: Modifier,
) {
    if (action.icon != null) {
        // An icon-only action names itself in a tooltip, as AppCompat's action items did.
        WithTooltip(action.title, modifier) { tooltip ->
            Box(
                Modifier
                    .combinedClickable(
                        enabled = action.enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 20.dp),
                        role = Role.Button,
                        onLongClick = tooltip::longPressed,
                        onClick = action.onClick,
                    ).testTag(action.id)
                    .size(48.dp)
                    .semantics { contentDescription = action.title },
                contentAlignment = ViewAlign.Center,
            ) {
                PlatformIcon(action.icon, tint = colors.onChrome, alpha = action.iconAlpha)
            }
        }
    } else {
        val base =
            modifier
                .clickable(
                    enabled = action.enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 20.dp),
                    role = Role.Button,
                    onClick = action.onClick,
                ).testTag(action.id)
        Box(
            base
                .height(48.dp)
                .widthIn(min = 48.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = ViewAlign.Center,
        ) {
            Text(
                action.title.uppercase(),
                style = TagMasterType.labelLarge.withoutLineHeight().copy(letterSpacing = 0.sp),
                color = if (action.enabled) colors.chromeActionText else colors.chromeActionText.copy(alpha = 0.38f),
                maxLines = 1,
            )
        }
    }
}

/**
 * The overflow popup, styled as the Popup overlay styled AppCompat's. It follows the app's
 * appearance, not the chrome's, so its press and focus highlights are the app surface's too.
 */
@Composable
private fun OverflowMenu(
    expanded: Boolean,
    items: List<BarAction>,
    onDismiss: () -> Unit,
) {
    val colors = TagMasterTheme.colors
    OnAppSurface {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            containerColor = colors.surfaceContainerHigh,
        ) {
            for (item in items) {
                DropdownMenuItem(
                    text = {
                        Text(
                            item.title,
                            style = TagMasterType.bodyLarge.withoutLineHeight(),
                            color = if (item.enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f),
                        )
                    },
                    enabled = item.enabled,
                    modifier = Modifier.testTag(item.id),
                    onClick = {
                        onDismiss()
                        item.onClick()
                    },
                )
            }
        }
    }
}

/** A drawable resource named by a theme attribute of the chrome overlay (the Up arrow). */
private fun themeDrawableRes(
    context: Context,
    attr: Int,
): Int {
    val chrome = android.view.ContextThemeWrapper(context, R.style.ThemeOverlay_TagMaster_Chrome)
    val value = android.util.TypedValue()
    chrome.theme.resolveAttribute(attr, value, true)
    return value.resourceId
}

/** The overflow button's icon, from the theme's `actionOverflowButtonStyle`. */
private fun overflowDrawableRes(context: Context): Int {
    val chrome = android.view.ContextThemeWrapper(context, R.style.ThemeOverlay_TagMaster_Chrome)
    val attrs = intArrayOf(android.R.attr.src, androidx.appcompat.R.attr.srcCompat)
    val array = chrome.obtainStyledAttributes(null, attrs, androidx.appcompat.R.attr.actionOverflowButtonStyle, 0)
    return try {
        array.getResourceId(1, 0).takeIf { it != 0 } ?: array.getResourceId(0, R.drawable.ic_more_vert)
    } finally {
        array.recycle()
    }
}
