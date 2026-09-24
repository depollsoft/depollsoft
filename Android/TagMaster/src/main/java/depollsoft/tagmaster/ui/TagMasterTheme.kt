package depollsoft.tagmaster.ui

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
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
import depollsoft.tagmaster.R

/**
 * Tag Master's colors, read from the activity's XML theme so the Compose screens paint exactly
 * the colors the View screens did, in light and dark.
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
            android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
    val chromeContext = android.view.ContextThemeWrapper(context, R.style.ThemeOverlay_TagMaster_Chrome)
    val onChrome = context.resColor(R.color.brand_on_chrome)
    return TagMasterColors(
        primary = context.attrColor(androidx.appcompat.R.attr.colorPrimary),
        onPrimary = context.attrColor(com.google.android.material.R.attr.colorOnPrimary),
        secondaryContainer = context.attrColor(com.google.android.material.R.attr.colorSecondaryContainer),
        onSecondaryContainer = context.attrColor(com.google.android.material.R.attr.colorOnSecondaryContainer),
        onSurface = context.attrColor(com.google.android.material.R.attr.colorOnSurface),
        onSurfaceVariant = context.attrColor(com.google.android.material.R.attr.colorOnSurfaceVariant),
        outline = context.attrColor(com.google.android.material.R.attr.colorOutline),
        outlineVariant = context.attrColor(com.google.android.material.R.attr.colorOutlineVariant),
        surface = context.attrColor(com.google.android.material.R.attr.colorSurface),
        surfaceContainerHigh = context.attrColor(com.google.android.material.R.attr.colorSurfaceContainerHigh),
        surfaceContainerHighest = context.attrColor(com.google.android.material.R.attr.colorSurfaceContainerHighest),
        background = context.attrColor(android.R.attr.colorBackground),
        error = context.attrColor(androidx.appcompat.R.attr.colorError),
        controlHighlight = context.attrColor(androidx.appcompat.R.attr.colorControlHighlight),
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

/** The Tag Master theme: the XML theme's colors, and Material 3 type set the way the Views set it. */
@Composable
fun TagMasterTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val colors = remember(context, configuration.uiMode) { tagMasterColors(context) }
    CompositionLocalProvider(LocalTagMasterColors provides colors) {
        MaterialTheme(colorScheme = colors.toColorScheme(), content = content)
    }
}

object TagMasterTheme {
    val colors: TagMasterColors
        @Composable get() = LocalTagMasterColors.current
}
