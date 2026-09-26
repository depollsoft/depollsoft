package depollsoft.tagmaster.ui

import depollsoft.compose.viewParagraph
import androidx.compose.material3.LocalTextStyle
import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.annotation.AttrRes
import androidx.appcompat.R as AppCompatR
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.material.R as MaterialR
import depollsoft.tagmaster.R

/**
 * Tag Master's colors, read from the activity's XML theme in light and dark.
 *
 * Material 3's text appearances color text with `android:textColorPrimary`, which the platform
 * theme resolves to Material's own neutral (#1D1B20 / #E6E0E9), not the app's `colorOnSurface`;
 * [text] keeps that distinction.
 */
@Immutable
data class TagMasterColors(
    val primary: Color,
    val onPrimary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val surface: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val background: Color,
    val error: Color,
    val controlHighlight: Color,
    /** The Chrome overlay's `colorControlHighlight`: the light ripple on the charcoal bars. */
    val chromeHighlight: Color,
    /** `android:textColorPrimary`: the color of every themed text appearance. */
    val text: Color,
    /** `android:textColorSecondary`. */
    val textSecondary: Color,
    val chrome: Color,
    val onChrome: Color,
    val chromeAccent: Color,
    /** Unselected tab labels and icons on the chrome. */
    val tabTint: Color,
    /** Action items on the chrome use the Chrome overlay's primary text color. */
    val chromeActionText: Color,
    val statusAvailable: Color,
    val watermark: Color,
    val sheetKeySurface: Color,
    val isDark: Boolean,
)

val LocalTagMasterColors = staticCompositionLocalOf<TagMasterColors> { error("No TagMasterTheme") }

private fun Context.attrColor(
    @AttrRes attr: Int,
): Color {
    val value = TypedValue()
    check(theme.resolveAttribute(attr, value, true)) { "Theme attribute $attr is not set" }
    val color =
        if (value.resourceId != 0) {
            ContextCompat.getColorStateList(this, value.resourceId)!!.defaultColor
        } else {
            value.data
        }
    return Color(color)
}

private fun Context.resColor(id: Int) = Color(ContextCompat.getColor(this, id))

fun tagMasterColors(context: Context): TagMasterColors {
    val dark =
        context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val chromeContext = ContextThemeWrapper(context, R.style.ThemeOverlay_TagMaster_Chrome)
    val onChrome = context.resColor(R.color.brand_on_chrome)
    return TagMasterColors(
        primary = context.attrColor(AppCompatR.attr.colorPrimary),
        onPrimary = context.attrColor(MaterialR.attr.colorOnPrimary),
        secondaryContainer = context.attrColor(MaterialR.attr.colorSecondaryContainer),
        onSecondaryContainer = context.attrColor(MaterialR.attr.colorOnSecondaryContainer),
        onSurface = context.attrColor(MaterialR.attr.colorOnSurface),
        onSurfaceVariant = context.attrColor(MaterialR.attr.colorOnSurfaceVariant),
        outline = context.attrColor(MaterialR.attr.colorOutline),
        outlineVariant = context.attrColor(MaterialR.attr.colorOutlineVariant),
        surface = context.attrColor(MaterialR.attr.colorSurface),
        surfaceContainerHigh = context.attrColor(MaterialR.attr.colorSurfaceContainerHigh),
        surfaceContainerHighest = context.attrColor(MaterialR.attr.colorSurfaceContainerHighest),
        background = context.attrColor(android.R.attr.colorBackground),
        error = context.attrColor(AppCompatR.attr.colorError),
        controlHighlight = context.attrColor(AppCompatR.attr.colorControlHighlight),
        chromeHighlight = chromeContext.attrColor(AppCompatR.attr.colorControlHighlight),
        text = context.attrColor(android.R.attr.textColorPrimary),
        textSecondary = context.attrColor(android.R.attr.textColorSecondary),
        chrome = context.resColor(R.color.brand_chrome),
        onChrome = onChrome,
        chromeAccent = context.resColor(R.color.brand_chrome_accent),
        tabTint = onChrome.copy(alpha = 0.7f),
        chromeActionText = chromeContext.attrColor(android.R.attr.textColorPrimary),
        statusAvailable = context.resColor(R.color.status_available),
        watermark = context.resColor(R.color.barberpole),
        sheetKeySurface = context.resColor(R.color.sheet_key_surface),
        isDark = dark,
    )
}

private fun TagMasterColors.toColorScheme(): ColorScheme =
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        surface = surface,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        background = background,
        onBackground = onSurface,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        error = error,
    )

/** The Tag Master theme: the XML theme's colors and Material 3 type. */
@Composable
fun TagMasterTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val colors = remember(context, configuration.uiMode) { tagMasterColors(context) }
    CompositionLocalProvider(LocalTagMasterColors provides colors) {
        MaterialTheme(colorScheme = colors.toColorScheme()) {
            // Text drawn with the default style lays out its paragraphs as a TextView did.
            CompositionLocalProvider(
                LocalRippleConfiguration provides colors.controlHighlight.asRipple(),
                LocalTextStyle provides LocalTextStyle.current.viewParagraph(),
                content = content,
            )
        }
    }
}

/**
 * A View's press and focus highlight is `colorControlHighlight`, alpha and all. A Material 3
 * ripple with no configuration takes the content color, black when nothing sets one, which vanishes
 * on the charcoal bars and in dark theme.
 */
@OptIn(ExperimentalMaterial3Api::class)
fun Color.asRipple(): RippleConfiguration {
    val alpha = alpha
    return RippleConfiguration(copy(alpha = 1f), RippleAlpha(draggedAlpha = alpha, focusedAlpha = alpha, hoveredAlpha = alpha, pressedAlpha = alpha))
}

/** [content] on the charcoal chrome: its ripples and focus highlights are the light ones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnChrome(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRippleConfiguration provides TagMasterTheme.colors.chromeHighlight.asRipple(), content = content)
}

/**
 * [content] back on the app's surface inside the chrome, as a popup opened from the toolbar is:
 * its ripples and focus highlights are the app's again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnAppSurface(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRippleConfiguration provides TagMasterTheme.colors.controlHighlight.asRipple(), content = content)
}

object TagMasterTheme {
    val colors: TagMasterColors
        @Composable get() = LocalTagMasterColors.current
}
