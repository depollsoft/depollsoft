package depollsoft.pitchperfect

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.SizeF
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The pitch pipe mounted on the home screen. Thirteen cells ring the readout
 * clockwise from C, just like the instrument; a tap sounds a note (toggle
 * mode, since a widget cannot hold), and the range selector switches octaves.
 */
class PitchPipeAppWidget : AppWidgetProvider() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            ACTION_SET_RANGE -> {
                // setIsFromFToF stops nothing itself; sounding notes keep sounding,
                // matching the instrument, which stops all on a range change.
                val model = PitchPipeModel()
                model.notes.forEach { it.stop() }
                model.isFromFToF = intent.getBooleanExtra(EXTRA_HIGH, false)
            }

            ACTION_TOGGLE_NOTE -> {
                // A broadcast, not a service: Android 12+ refuses background
                // service starts from a widget tap, and a manifest receiver is
                // always deliverable. Render straight away rather than through
                // the coalescing handler so the cell lights on this tap.
                val accidental =
                    when (intent.getStringExtra(EXTRA_ACCIDENTAL)) {
                        "#" -> Accidental.Sharp
                        "b" -> Accidental.Flat
                        else -> Accidental.Natural
                    }
                val note =
                    Note.findNote(
                        intent.getStringExtra(EXTRA_NOTE_NAME) ?: "C",
                        accidental,
                        intent.getIntExtra(EXTRA_OCTAVE, 4),
                    )
                val startedAt = SystemClock.elapsedRealtime()
                if (note.isPlaying) note.stop() else note.play()
                PerformanceDiagnostics.logDuration("Widget note toggled", startedAt, "playing=${note.isPlaying}")
                mainHandler.removeCallbacks(pendingUpdate)
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, PitchPipeAppWidget::class.java))
                if (ids.isNotEmpty()) renderAsync(context, manager, ids)
            }
        }
        super.onReceive(context, intent)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        renderAsync(context, appWidgetManager, intArrayOf(appWidgetId))
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        renderAsync(context, appWidgetManager, appWidgetIds)
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    /**
     * The system delivers widget broadcasts on the app's main thread, so each
     * render (one face plus thirteen cells per size) used to stall the UI.
     */
    private fun renderAsync(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
    ) {
        val pending = goAsync()
        val appContext = context.applicationContext
        RENDER_EXECUTOR.execute {
            try {
                render(appContext, manager, ids)
            } finally {
                pending?.finish()
            }
        }
    }

    internal fun render(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
    ) {
        val startedAt = SystemClock.elapsedRealtime()
        for (id in ids) {
            manager.updateAppWidget(id, build(context, manager, id))
        }
        PerformanceDiagnostics.logDuration("Widget update rendered", startedAt, "widgets=${ids.size}")
    }

    private fun build(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
    ): RemoteViews {
        val options = manager.getAppWidgetOptions(appWidgetId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sizes = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            if (!sizes.isNullOrEmpty()) {
                return RemoteViews(
                    sizes.associateWith { size ->
                        buildForSize(context, size.width, size.height, positionTargets = true)
                    },
                )
            }
        }
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: 280
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).takeIf { it > 0 } ?: 280
        return buildForSize(context, width.toFloat(), height.toFloat(), positionTargets = false)
    }

    private fun buildForSize(
        context: Context,
        widthDp: Float,
        heightDp: Float,
        positionTargets: Boolean,
    ): RemoteViews {
        val model = PitchPipeModel()
        val notes = model.notes.toList()
        val density = context.resources.displayMetrics.density
        val faceDp = min(widthDp, heightDp)
        val facePx = (faceDp * density).roundToInt().coerceAtLeast(1)
        val renderContext = themedContext(context)
        val face = min(widthDp, heightDp)
        val ring = face * 0.365f
        val cellTarget = maxOf(52f, ring * 0.45f)
        val rangeWidth = maxOf(132f, ring * 0.9f)
        val rangeHeight = maxOf(56f, ring * 0.32f)
        val views = RemoteViews(context.packageName, R.layout.pitchpipewidgetview)
        views.setInt(
            R.id.widgetRoot,
            "setBackgroundColor",
            ContextCompat.getColor(renderContext, R.color.plate_ground),
        )
        views.setImageViewBitmap(
            R.id.widgetFace,
            PitchPipeWidgetRenderer.face(
                renderContext,
                notes,
                model.isFromFToF,
                facePx,
                facePx,
                drawControls = !positionTargets,
                bloomCellPx = if (positionTargets) cellTarget * density * 0.44f else 0f,
            ),
        )
        CELL_IDS.forEachIndexed { index, id ->
            val note = notes[index]
            views.setContentDescription(id, spokenName(note))
            views.setOnClickPendingIntent(id, noteIntent(context, note, index))
            if (positionTargets) {
                val pixels = (cellTarget * density).roundToInt()
                views.setImageViewBitmap(
                    id,
                    PitchPipeWidgetRenderer.cell(renderContext, note, pixels),
                )
            }
        }
        val high = model.isFromFToF
        views.setContentDescription(
            R.id.rangeToggle,
            context.getString(if (high) R.string.widget_switch_to_c else R.string.widget_switch_to_f),
        )
        views.setOnClickPendingIntent(R.id.rangeToggle, rangeIntent(context, !high))
        if (positionTargets) {
            views.setImageViewBitmap(
                R.id.rangeToggle,
                PitchPipeWidgetRenderer.rangeSelector(
                    renderContext,
                    high,
                    (rangeWidth * density).roundToInt(),
                    (rangeHeight * density).roundToInt(),
                ),
            )
            positionModernHitTargets(
                views,
                widthDp,
                heightDp,
                notes.size,
                cellTarget,
                rangeWidth,
                rangeHeight,
            )
        }
        return views
    }

    private fun positionModernHitTargets(
        views: RemoteViews,
        widgetWidth: Float,
        widgetHeight: Float,
        count: Int,
        target: Float,
        toggleWidth: Float,
        toggleHeight: Float,
    ) {
        val face = min(widgetWidth, widgetHeight)
        val originX = (widgetWidth - face) / 2f
        val originY = (widgetHeight - face) / 2f
        val centerX = originX + face / 2f
        val centerY = originY + face * PitchPipeWidgetRenderer.FACE_CENTER_Y_FRACTION
        val ring = face * 0.365f
        val step = 360.0 / count.coerceAtLeast(1)
        val start = -90.0 + step / 2.0
        CELL_IDS.forEachIndexed { index, id ->
            val angle = Math.toRadians(start + index * step)
            val x = centerX + (cos(angle) * ring).toFloat() - target / 2f
            val y = centerY + (sin(angle) * ring).toFloat() - target / 2f
            views.setViewLayoutWidth(id, target, TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutHeight(id, target, TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutMargin(id, RemoteViews.MARGIN_LEFT, x, TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutMargin(id, RemoteViews.MARGIN_TOP, y, TypedValue.COMPLEX_UNIT_DIP)
        }
        views.setViewLayoutWidth(R.id.rangeToggle, toggleWidth, TypedValue.COMPLEX_UNIT_DIP)
        views.setViewLayoutHeight(R.id.rangeToggle, toggleHeight, TypedValue.COMPLEX_UNIT_DIP)
        views.setViewLayoutMargin(
            R.id.rangeToggle,
            RemoteViews.MARGIN_LEFT,
            centerX - toggleWidth / 2f,
            TypedValue.COMPLEX_UNIT_DIP,
        )
        views.setViewLayoutMargin(
            R.id.rangeToggle,
            RemoteViews.MARGIN_TOP,
            centerY + ring * 0.20f,
            TypedValue.COMPLEX_UNIT_DIP,
        )
    }

    private fun themedContext(context: Context): Context {
        val nightMode =
            when (PitchPerfectApplication.themeMode) {
                AppCompatDelegate.MODE_NIGHT_YES -> Configuration.UI_MODE_NIGHT_YES
                AppCompatDelegate.MODE_NIGHT_NO -> Configuration.UI_MODE_NIGHT_NO
                else -> return context
            }
        val configuration = Configuration(context.resources.configuration)
        configuration.uiMode =
            (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        return context.createConfigurationContext(configuration)
    }

    private fun spokenName(note: Note): String =
        when (note.accidental) {
            Accidental.Natural -> "${note.friendlyName}, octave ${note.octave}"
            else -> "${note.friendlyName} sharp, octave ${note.octave}"
        }

    private fun noteIntent(
        context: Context,
        note: Note,
        index: Int,
    ): PendingIntent {
        val intent =
            Intent(context, PitchPipeAppWidget::class.java)
                .setAction(ACTION_TOGGLE_NOTE)
                .putExtra(EXTRA_NOTE_NAME, note.friendlyName)
                .putExtra(EXTRA_OCTAVE, note.octave)
        when (note.accidental) {
            Accidental.Flat -> intent.putExtra(EXTRA_ACCIDENTAL, "b")
            Accidental.Sharp -> intent.putExtra(EXTRA_ACCIDENTAL, "#")
            else -> Unit
        }
        return PendingIntent.getBroadcast(
            context,
            index,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun rangeIntent(
        context: Context,
        high: Boolean,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            if (high) 101 else 100,
            Intent(context, PitchPipeAppWidget::class.java)
                .setAction(ACTION_SET_RANGE)
                .putExtra(EXTRA_HIGH, high),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val ACTION_SET_RANGE = "depollsoft.pitchperfect.widget.SET_RANGE"
        const val ACTION_TOGGLE_NOTE = "depollsoft.pitchperfect.widget.TOGGLE_NOTE"
        const val EXTRA_HIGH = "high"
        const val EXTRA_NOTE_NAME = "noteName"
        const val EXTRA_ACCIDENTAL = "accidental"
        const val EXTRA_OCTAVE = "octave"

        // Clockwise from C at the top-left, around the grid's perimeter.
        private val CELL_IDS =
            intArrayOf(
                R.id.pitchButton0,
                R.id.pitchButton1,
                R.id.pitchButton2,
                R.id.pitchButton3,
                R.id.pitchButton4,
                R.id.pitchButton5,
                R.id.pitchButton6,
                R.id.pitchButton7,
                R.id.pitchButton8,
                R.id.pitchButton9,
                R.id.pitchButton10,
                R.id.pitchButton11,
                R.id.pitchButton12,
            )

        private const val UPDATE_COALESCE_MS = 40L
        private val RENDER_EXECUTOR =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "widget-render").apply { isDaemon = true }
            }
        private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
        private val renderer by lazy { PitchPipeAppWidget() }
        private val pendingUpdate =
            Runnable {
                val context = RichApplication.getAppContext() ?: return@Runnable
                RENDER_EXECUTOR.execute {
                    val manager = AppWidgetManager.getInstance(context)
                    val ids = manager.getAppWidgetIds(ComponentName(context, PitchPipeAppWidget::class.java))
                    if (ids.isEmpty()) return@execute
                    renderer.render(context, manager, ids)
                }
            }

        /**
         * Coalesces bursts (a page change stops every note on it) into one
         * render off the main thread, without a broadcast round trip.
         */
        @JvmStatic
        fun updateWidgets() {
            mainHandler.removeCallbacks(pendingUpdate)
            mainHandler.postDelayed(pendingUpdate, UPDATE_COALESCE_MS)
        }
    }
}
