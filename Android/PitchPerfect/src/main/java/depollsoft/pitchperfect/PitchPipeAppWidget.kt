package depollsoft.pitchperfect

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatDelegate
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import kotlin.math.roundToInt

/**
 * The pitch pipe mounted on the home screen. Twelve cells ring the readout
 * clockwise from C, just like the instrument; a tap sounds a note (toggle
 * mode, since a widget cannot hold), and the range selector switches octaves.
 */
class PitchPipeAppWidget : AppWidgetProvider() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == ACTION_SET_RANGE) {
            // setIsFromFToF stops nothing itself; sounding notes keep sounding,
            // matching the instrument, which stops all on a range change.
            val model = PitchPipeModel()
            model.notes.forEach { it.stop() }
            model.isFromFToF = intent.getBooleanExtra(EXTRA_HIGH, false)
        }
        super.onReceive(context, intent)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        appWidgetManager.updateAppWidget(appWidgetId, build(context, appWidgetManager, appWidgetId))
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, build(context, appWidgetManager, id))
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun build(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
    ): RemoteViews {
        val model = PitchPipeModel()
        val notes = model.notes.toList()
        val density = context.resources.displayMetrics.density
        val options = manager.getAppWidgetOptions(appWidgetId)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: 220
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).takeIf { it > 0 } ?: 220
        val faceW = (widthDp * density).roundToInt().coerceAtLeast(1)
        val faceH = (heightDp * density).roundToInt().coerceAtLeast(1)

        val renderContext = themedContext(context)
        val rv = RemoteViews(context.packageName, R.layout.pitchpipewidgetview)
        rv.setImageViewBitmap(
            R.id.widgetFace,
            PitchPipeWidgetRenderer.face(renderContext, notes, model.isFromFToF, faceW, faceH),
        )

        CELL_IDS.forEachIndexed { index, id ->
            val note = notes[index]
            rv.setContentDescription(id, spokenName(note))
            rv.setOnClickPendingIntent(id, noteIntent(context, note, index))
        }

        rv.setContentDescription(R.id.readout, context.getString(R.string.widget_open_app))
        rv.setContentDescription(R.id.readout, context.getString(R.string.widget_open_app))
        rv.setOnClickPendingIntent(
            R.id.readout,
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, PitchPerfectActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )

        rv.setContentDescription(R.id.rangeLow, context.getString(R.string.RangeLowDescription))
        rv.setContentDescription(R.id.rangeHigh, context.getString(R.string.RangeHighDescription))
        rv.setOnClickPendingIntent(R.id.rangeLow, rangeIntent(context, false))
        rv.setOnClickPendingIntent(R.id.rangeHigh, rangeIntent(context, true))
        return rv
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
            Intent(context, PitchPerfectService::class.java)
                .putExtra("noteName", note.friendlyName)
                .putExtra("octave", note.octave)
        when (note.accidental) {
            Accidental.Flat -> intent.putExtra("accidental", "b")
            Accidental.Sharp -> intent.putExtra("accidental", "#")
            else -> Unit
        }
        return PendingIntent.getService(
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
        const val EXTRA_HIGH = "high"

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
            )

        @JvmStatic
        fun updateWidgets() {
            val context = RichApplication.getAppContext() ?: return
            val ids =
                AppWidgetManager
                    .getInstance(context)
                    .getAppWidgetIds(ComponentName(context, PitchPipeAppWidget::class.java))
            if (ids.isEmpty()) return
            val intent =
                Intent(context, PitchPipeAppWidget::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
