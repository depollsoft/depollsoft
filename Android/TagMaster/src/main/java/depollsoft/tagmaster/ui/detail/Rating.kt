package depollsoft.tagmaster.ui.detail

import android.content.Context
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import depollsoft.tagmaster.ui.TagMasterTheme
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val STARS = 5

/**
 * The stars of a RatingBar in the style [styleAttr] names, built the way ProgressBar builds them:
 * the style's progress drawable, each bitmap layer repeating across the bar and the progress
 * layers clipped to the rating, the progress layer tinted with [progressTint].
 */
internal class StarBar(
    context: Context,
    styleAttr: Int,
    progressTint: Int?,
) {
    private val drawable: LayerDrawable
    private var sampleWidth = 0
    val height: Int

    /** The bar's width: one sample star per star. */
    val width: Int get() = sampleWidth * STARS

    init {
        val array = context.obtainStyledAttributes(null, intArrayOf(android.R.attr.progressDrawable), styleAttr, 0)
        val source = array.getDrawable(0)!!
        array.recycle()
        drawable = tileify(source, false, context) as LayerDrawable
        if (progressTint != null) drawable.findDrawableByLayerId(android.R.id.progress)?.setTint(progressTint)
        height = drawable.intrinsicHeight
    }

    fun draw(
        canvas: android.graphics.Canvas,
        rating: Float,
    ) {
        val level = (rating / STARS * 10000).roundToInt().coerceIn(0, 10000)
        drawable.findDrawableByLayerId(android.R.id.progress)?.level = level
        drawable.findDrawableByLayerId(android.R.id.secondaryProgress)?.level = 0
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
    }

    private fun tileify(
        source: Drawable,
        clip: Boolean,
        context: Context,
    ): Drawable =
        when (source) {
            is LayerDrawable -> {
                val count = source.numberOfLayers
                val layers =
                    Array(count) { index ->
                        val id = source.getId(index)
                        tileify(source.getDrawable(index), id == android.R.id.progress || id == android.R.id.secondaryProgress, context)
                    }
                LayerDrawable(layers).also { result ->
                    for (index in 0 until count) {
                        result.setId(index, source.getId(index))
                        result.setLayerGravity(index, source.getLayerGravity(index))
                        result.setLayerWidth(index, source.getLayerWidth(index))
                        result.setLayerHeight(index, source.getLayerHeight(index))
                        result.setLayerInsetLeft(index, source.getLayerInsetLeft(index))
                        result.setLayerInsetRight(index, source.getLayerInsetRight(index))
                        result.setLayerInsetTop(index, source.getLayerInsetTop(index))
                        result.setLayerInsetBottom(index, source.getLayerInsetBottom(index))
                        result.setLayerInsetStart(index, source.getLayerInsetStart(index))
                        result.setLayerInsetEnd(index, source.getLayerInsetEnd(index))
                    }
                }
            }
            is BitmapDrawable -> {
                val clone = source.constantState!!.newDrawable(context.resources) as BitmapDrawable
                clone.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
                if (sampleWidth <= 0) sampleWidth = clone.intrinsicWidth
                if (clip) ClipDrawable(clone, Gravity.LEFT, ClipDrawable.HORIZONTAL) else clone
            }
            else -> source
        }
}

/** The small, read-only star rating of the Summary page (ratingBarStyleSmall, primary stars). */
@Composable
fun RatingStars(
    rating: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val primary = TagMasterTheme.colors.primary.toArgb()
    val bar = remember(context, configuration.uiMode, primary) { StarBar(context, android.R.attr.ratingBarStyleSmall, primary) }
    val density = LocalDensity.current
    Box(
        modifier
            .width(with(density) { bar.width.toDp() })
            .height(with(density) { bar.height.toDp() })
            .drawBehind { drawIntoCanvas { bar.draw(it.nativeCanvas, rating) } },
    )
}

/**
 * The rating dialog's stars: 1 to 5, set by tapping or dragging across them, or with the arrow
 * keys, and adjustable by a screen reader.
 */
@Composable
fun RatingPicker(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val bar = remember(context, configuration.uiMode) { StarBar(context, androidx.appcompat.R.attr.ratingBarStyle, null) }
    val density = LocalDensity.current

    fun ratingAt(x: Float): Int = ceil(x / (bar.width / STARS.toFloat())).toInt().coerceIn(0, STARS)
    Box(
        modifier
            .width(with(density) { bar.width.toDp() })
            .height(with(density) { bar.height.toDp() })
            .pointerInput(bar) { detectTapGestures { onRatingChange(ratingAt(it.x)) } }
            .pointerInput(bar) { detectHorizontalDragGestures { change, _ -> onRatingChange(ratingAt(change.position.x)) } }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionRight -> onRatingChange((rating + 1).coerceAtMost(STARS)).let { true }
                    Key.DirectionLeft -> onRatingChange((rating - 1).coerceAtLeast(0)).let { true }
                    else -> false
                }
            }.focusable()
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(rating.toFloat(), 0f..STARS.toFloat(), STARS - 1)
                setProgress { value ->
                    onRatingChange(value.roundToInt().coerceIn(0, STARS))
                    true
                }
            }.drawBehind { drawIntoCanvas { bar.draw(it.nativeCanvas, rating.toFloat()) } },
    )
}
