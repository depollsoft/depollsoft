package depollsoft.tagmaster.ui

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import depollsoft.tagmaster.BarberPoleLogo
import depollsoft.tagmaster.QuartetArtwork
import kotlin.math.floor
import kotlin.math.roundToInt

/** The barber-pole loader's artwork, drawn in the logo's own coordinates. */
internal object BarberPoleRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val logo = BarberPoleLogo(Resources.getSystem())

    /**
     * Draws the pole centered in [width] x [height] pixels. The stripes move with [stripePhase]
     * (0..1); the frame never rotates. [compactHeightPx] caps the scale for the compact loader.
     */
    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        stripePhase: Float,
        dark: Boolean,
        compactHeightPx: Float?,
    ) {
        val saved = canvas.save()
        val fit = minOf(width / BarberPoleLogo.WIDTH, height / BarberPoleLogo.HEIGHT)
        val scale = if (compactHeightPx != null) minOf(fit, compactHeightPx / BarberPoleLogo.HEIGHT) else fit
        canvas.translate((width - BarberPoleLogo.WIDTH * scale) / 2f, (height - BarberPoleLogo.HEIGHT * scale) / 2f)
        canvas.scale(scale, scale)
        paint.style = Paint.Style.FILL
        paint.color = if (dark) BarberPoleLogo.METAL_DARK else BarberPoleLogo.METAL_LIGHT
        canvas.drawPath(logo.metal, paint)
        paint.color = BarberPoleLogo.WHITE
        canvas.drawPath(logo.shaft, paint)
        val cylinder = canvas.save()
        canvas.clipPath(logo.shaft)
        // In the logo's own coordinate system only stripe phase moves. The frame never rotates.
        canvas.rotate(BarberPoleLogo.AXIS_ANGLE)
        val phase = stripePhase - floor(stripePhase)
        for (index in BarberPoleLogo.REPEAT_MIN..BarberPoleLogo.REPEAT_MAX) {
            val band = canvas.save()
            canvas.translate(0f, (index + phase * BarberPoleLogo.PHASE_MULTIPLIER) * BarberPoleLogo.STRIPE_STEP)
            paint.color = if (index % 2 == 0) BarberPoleLogo.RED else BarberPoleLogo.BLUE
            canvas.drawPath(logo.stripe, paint)
            canvas.restoreToCount(band)
        }
        canvas.restoreToCount(cylinder)
        canvas.restoreToCount(saved)
    }
}

/** The quartet illustration of the tag-detail loading state. */
internal object QuartetRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        phase: Float,
        moving: Boolean,
        dark: Boolean,
    ) {
        val save = canvas.save()
        val scale = minOf(width / QuartetArtwork.WIDTH, height / QuartetArtwork.HEIGHT)
        canvas.translate((width - QuartetArtwork.WIDTH * scale) / 2, (height - QuartetArtwork.HEIGHT * scale) / 2)
        canvas.scale(scale, scale)
        paint.color = if (dark) QuartetArtwork.darkStaff else QuartetArtwork.lightStaff
        paint.alpha = (QuartetArtwork.STAFFALPHA * 255).toInt()
        paint.strokeWidth = QuartetArtwork.STAFFWIDTH
        paint.style = Paint.Style.STROKE
        val staffSave = canvas.save()
        canvas.clipPath(QuartetArtwork.staffmask)
        canvas.drawPath(QuartetArtwork.staff, paint)
        paint.color = if (dark) QuartetArtwork.darkNote else QuartetArtwork.lightNote
        paint.alpha = 255
        paint.style = Paint.Style.FILL
        canvas.drawPath(QuartetArtwork.stem, paint)
        canvas.drawPath(QuartetArtwork.ledger, paint)
        canvas.drawPath(QuartetArtwork.flat, paint)
        canvas.drawPath(QuartetArtwork.label, paint)
        canvas.restoreToCount(staffSave)
        for (voice in QuartetArtwork.x.indices) {
            val noteSave = canvas.save()
            val opacity = if (moving) QuartetArtwork.opacity(voice, phase) else QuartetArtwork.still[voice]
            paint.alpha = (opacity * 255).roundToInt()
            canvas.translate(QuartetArtwork.x[voice], QuartetArtwork.y[voice])
            canvas.drawPath(QuartetArtwork.note, paint)
            canvas.restoreToCount(noteSave)
        }
        canvas.restoreToCount(save)
    }
}

/** Whether the system lets things move: Settings' animator duration scale is not zero. */
@Composable
fun motionAllowed(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

/** Whether something asked to move ([wanted]) may: the host is resumed and motion is allowed. */
@Composable
fun motionRunning(wanted: Boolean): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycle.value.isAtLeast(Lifecycle.State.RESUMED)
    return wanted && resumed && motionAllowed() && !LocalInspectionMode.current
}

/** A looping phase from [from] to [to] while [running]; otherwise it rests at [rest]. */
@Composable
fun loopingPhase(
    running: Boolean,
    durationMillis: Int,
    from: Float = 0f,
    to: Float = 1f,
    rest: Float = 0f,
): State<Float> {
    if (!running) return remember(rest) { mutableFloatStateOf(rest) }
    val transition = rememberInfiniteTransition(label = "loop")
    return transition.animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
}

/**
 * The compact barber-pole progress indicator (18.76 x 32dp). It keeps its space while idle, as
 * the View did with INVISIBLE, and says [description] while [loading].
 */
@Composable
fun CompactBarberPole(
    loading: Boolean,
    description: String,
    modifier: Modifier = Modifier,
) {
    val dark = TagMasterTheme.colors.isDark
    val phase by loopingPhase(motionRunning(loading), (BarberPoleLogo.DURATION_SECONDS * 1000).toInt())
    Box(
        modifier
            .size(BarberPoleLogo.COMPACT_WIDTH.dp, BarberPoleLogo.COMPACT_HEIGHT.dp)
            .then(if (loading) Modifier.semantics { contentDescription = description } else Modifier)
            .drawBehind {
                if (!loading) return@drawBehind
                drawIntoCanvas {
                    BarberPoleRenderer.draw(
                        it.nativeCanvas,
                        size.width.roundToInt(),
                        size.height.roundToInt(),
                        phase,
                        dark,
                        BarberPoleLogo.COMPACT_HEIGHT * density,
                    )
                }
            },
    )
}

/** The full-size barber-pole loader (34 x 58dp), present only while [loading]. */
@Composable
fun BarberPoleLoader(
    loading: Boolean,
    description: String,
    modifier: Modifier = Modifier,
) {
    if (!loading) return
    val dark = TagMasterTheme.colors.isDark
    val phase by loopingPhase(motionRunning(true), (BarberPoleLogo.DURATION_SECONDS * 1000).toInt())
    Box(
        modifier
            .size(BarberPoleLogo.ARTWORK_WIDTH.dp, BarberPoleLogo.ARTWORK_HEIGHT.dp)
            .semantics { contentDescription = description }
            .drawBehind {
                drawIntoCanvas {
                    BarberPoleRenderer.draw(it.nativeCanvas, size.width.roundToInt(), size.height.roundToInt(), phase, dark, null)
                }
            },
    )
}

/** The decorative quartet: still unless [loading], when its notes take turns. */
@Composable
fun QuartetIllustration(
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    val dark = TagMasterTheme.colors.isDark
    val moving = motionRunning(loading)
    val phase by loopingPhase(moving, (QuartetArtwork.PERIOD * 1000).toInt(), QuartetArtwork.PHASESTART, QuartetArtwork.PHASEEND)
    Box(
        modifier
            .size(QuartetArtwork.WIDTH.dp, QuartetArtwork.HEIGHT.dp)
            .drawBehind {
                drawIntoCanvas {
                    QuartetRenderer.draw(it.nativeCanvas, size.width.roundToInt(), size.height.roundToInt(), phase, moving, dark)
                }
            },
    )
}
