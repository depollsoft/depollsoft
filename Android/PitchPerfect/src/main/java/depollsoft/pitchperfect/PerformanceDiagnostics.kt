package depollsoft.pitchperfect

import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.FrameMetrics
import android.view.Window
import depollsoft.lib.util.AppLog
import java.util.concurrent.Executors

/** PII-free timing diagnostics included only in private preview builds. */
internal object PerformanceDiagnostics {
    private val logger = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "performance-log").apply { isDaemon = true }
    }

    val enabled: Boolean
        get() = BuildConfig.PRIVATE_BUILD_NUMBER.isNotBlank()

    fun logDuration(
        operation: String,
        startedAt: Long,
        details: String = "",
    ) {
        if (!enabled) return
        val suffix = if (details.isBlank()) "" else "; $details"
        val message = "$operation in ${SystemClock.elapsedRealtime() - startedAt} ms$suffix"
        logger.execute { AppLog.info("Performance", message) }
    }
}

/** Samples rendered frame durations without doing work on the UI thread. */
internal class FramePerformanceMonitor(
    private val screen: String,
) {
    private val thread = HandlerThread("$screen-frame-metrics")
    private var frames = 0
    private var slowFrames = 0
    private var frozenFrames = 0
    private var maxDurationMs = 0L
    private var started = false

    private val listener =
        Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            val durationNs = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
            if (durationNs <= 0) return@OnFrameMetricsAvailableListener
            val durationMs = durationNs / 1_000_000L
            frames++
            if (durationMs > 16L) slowFrames++
            if (durationMs > 700L) frozenFrames++
            maxDurationMs = maxOf(maxDurationMs, durationMs)
            if (frames % REPORT_INTERVAL == 0) report("sample")
        }

    fun start(window: Window) {
        if (started) return
        started = true
        thread.start()
        window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
    }

    fun stop(window: Window) {
        if (!started) return
        window.removeOnFrameMetricsAvailableListener(listener)
        report("stop")
        thread.quitSafely()
        started = false
    }

    private fun report(reason: String) {
        if (frames == 0) return
        AppLog.info(
            "Performance",
            "$screen frames ($reason): total=$frames slow=$slowFrames frozen=$frozenFrames max=${maxDurationMs}ms",
        )
    }

    private companion object {
        const val REPORT_INTERVAL = 120
    }
}
