package depollsoft.pitchperfect.ui

import android.content.Context
import android.graphics.Typeface
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import depollsoft.compose.inWholePixels
import depollsoft.pitchperfect.R

/**
 * The Laboratory Instrument palette: grayscale plates with one lit accent.
 *
 * Read from the color resources so the night variant (`values-night`) and the widget, which
 * still draws with the same resources, stay in step.
 */
@Immutable
data class PlateColors(
    val ground: Color,
    val surface: Color,
    val ink: Color,
    val inkSecondary: Color,
    val hairline: Color,
    val accent: Color,
    val onAccent: Color,
)

val LocalPlateColors =
    staticCompositionLocalOf<PlateColors> { error("PlateTheme is not in the composition") }

/** The plate's colors, inside [PlateTheme]. */
val plateColors: PlateColors
    @Composable get() = LocalPlateColors.current

/** The app's typefaces. */
object PlateFonts {
    /** `@font/oswald_medium`: engraved captions, titles and buttons. */
    val oswald = FontFamily(Font(R.font.oswald_medium, FontWeight.Medium))

    /** `sans-serif-condensed`: note and song names. */
    val condensedTypeface: Typeface =
        Typeface.create("sans-serif-condensed", Typeface.NORMAL)

    val condensed = FontFamily(androidx.compose.ui.text.font.Typeface(condensedTypeface))

    /** `monospace`: frequencies, keys and section headers. */
    val mono = FontFamily.Monospace

    /** The Oswald face as a platform typeface, for text drawn outside Compose's text layout. */
    fun oswaldTypeface(context: Context): Typeface =
        ResourcesCompat.getFont(context, R.font.oswald_medium)
            ?: Typeface.create("sans-serif-condensed", Typeface.NORMAL)
}

/**
 * A TextView's text: font padding included, so a line is exactly as tall and its baseline exactly
 * where the View layouts put it.
 *
 * A TextView reads a size from XML as whole pixels (`getDimensionPixelSize` rounds 16sp at 2.625x,
 * 42px exactly, but 15sp, 39.375px, to 39px); on a device whose density is not a whole number the
 * fraction would make every line differ. Pass [wholePixels] false for a size the View code set with
 * `setTextSize`, which keeps the fraction.
 */
@Suppress("DEPRECATION")
@Composable
@ReadOnlyComposable
fun plateText(
    size: TextUnit,
    color: Color = Color.Unspecified,
    family: FontFamily = FontFamily.Default,
    weight: FontWeight? = null,
    letterSpacing: Float = 0f,
    style: FontStyle? = null,
    wholePixels: Boolean = true,
): TextStyle =
    TextStyle(
        color = color,
        fontSize = if (wholePixels) size.inWholePixels(LocalDensity.current) else size,
        fontFamily = family,
        fontWeight = weight,
        fontStyle = style,
        letterSpacing = letterSpacing.em,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    )

@Composable
fun PlateTheme(content: @Composable () -> Unit) {
    val colors =
        PlateColors(
            ground = colorResource(R.color.plate_ground),
            surface = colorResource(R.color.plate_surface),
            ink = colorResource(R.color.plate_ink),
            inkSecondary = colorResource(R.color.plate_ink_secondary),
            hairline = colorResource(R.color.plate_hairline),
            accent = colorResource(R.color.plate_accent),
            onAccent = colorResource(R.color.plate_on_accent),
        )
    // Controls borrowed from Material take the ink as their active color.
    val material =
        lightColors(
            primary = colors.ink,
            primaryVariant = colors.ink,
            secondary = colors.ink,
            onSecondary = colors.ground,
            surface = colors.surface,
            onSurface = colors.ink,
            background = colors.ground,
            onBackground = colors.ink,
            error = colors.ink,
            onError = colors.ground,
        )
    CompositionLocalProvider(LocalPlateColors provides colors) {
        MaterialTheme(colors = material, content = content)
    }
}
