package depollsoft.pitchperfect

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.view.FrameMetrics
import android.view.Window
import androidx.profileinstaller.ProfileVerifier
import depollsoft.lib.util.AppLog
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** PII-free timing diagnostics included only in private preview builds. */
internal object PerformanceDiagnostics {
    private val logger =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "performance-log").apply { isDaemon = true }
        }
    private val compilationLogged = AtomicBoolean(false)
    private val mainThreadMonitorStarted = AtomicBoolean(false)

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

    /**
     * Names any main-looper message over [SLOW_MESSAGE_MS] by its Handler and
     * callback class. Frame stats file such time under "unknown delay".
     */
    fun startMainThreadMonitor() {
        if (!enabled || !mainThreadMonitorStarted.compareAndSet(false, true)) return
        var startedAt = 0L
        var dispatched: String? = null
        Looper.getMainLooper().setMessageLogging { line ->
            if (line.startsWith(">>>>>")) {
                startedAt = SystemClock.uptimeMillis()
                dispatched = line
            } else if (line.startsWith("<<<<<")) {
                val elapsed = SystemClock.uptimeMillis() - startedAt
                val message = dispatched
                dispatched = null
                if (elapsed >= SLOW_MESSAGE_MS && message != null) {
                    logger.execute {
                        AppLog.info("Performance", "Slow main-thread message: $elapsed ms $message")
                    }
                }
            }
        }
    }

    private const val SLOW_MESSAGE_MS = 24L

    /** Reports whether ART compiled this install with the shipped baseline profile. */
    fun logCompilationStatusOnce() {
        if (!enabled || !compilationLogged.compareAndSet(false, true)) return
        logger.execute {
            val message =
                runCatching {
                    val status =
                        ProfileVerifier.getCompilationStatusAsync().get(20, TimeUnit.SECONDS)
                    "Compilation status: code=${status.profileInstallResultCode} " +
                        "compiledWithProfile=${status.isCompiledWithProfile} " +
                        "profileEnqueued=${status.hasProfileEnqueuedForCompilation()}"
                }.getOrElse { "Compilation status unavailable: ${it.javaClass.simpleName}" }
            AppLog.info("Performance", message)
        }
    }
}

/** Sums per-stage timings of slow frames and keeps the worst few for the log. */
internal class FrameStageAccumulator {
    private val stageTotalsMs = LongArray(STAGE_NAMES.size)
    private val worst = ArrayList<Pair<Long, LongArray>>(WORST_FRAMES + 1)
    var slowFrames = 0
        private set

    fun add(
        durationMs: Long,
        stagesMs: LongArray,
    ) {
        slowFrames++
        for (i in stageTotalsMs.indices) stageTotalsMs[i] += stagesMs.getOrElse(i) { 0L }
        worst.add(durationMs to stagesMs.copyOf())
        worst.sortByDescending { it.first }
        while (worst.size > WORST_FRAMES) worst.removeAt(worst.size - 1)
    }

    fun describe(): String {
        if (slowFrames == 0) return ""
        val totals =
            STAGE_NAMES.indices
                .filter { stageTotalsMs[it] > 0 }
                .sortedByDescending { stageTotalsMs[it] }
                .joinToString(" ") { "${STAGE_NAMES[it]}=${stageTotalsMs[it]}" }
        val worstFrames =
            worst.joinToString("; ") { (total, stages) ->
                val parts =
                    STAGE_NAMES.indices
                        .filter { stages[it] >= 2 }
                        .sortedByDescending { stages[it] }
                        .joinToString(",") { "${STAGE_NAMES[it]}=${stages[it]}" }
                "${total}ms{$parts}"
            }
        return " slowStagesMs[$totals] worst[$worstFrames]"
    }

    companion object {
        val STAGE_NAMES =
            arrayOf("unknown", "input", "anim", "layout", "draw", "sync", "gpuCmd", "swap", "gpu")
        const val WORST_FRAMES = 3
    }
}

/** Samples rendered frame durations without doing work on the UI thread. */
internal class FramePerformanceMonitor(
    private val screen: String,
) {
    private val thread = HandlerThread("$screen-frame-metrics")
    private var frames = 0
    private var over8MsFrames = 0
    private var over16MsFrames = 0
    private var over32MsFrames = 0
    private var frozenFrames = 0
    private var maxDurationMs = 0L
    private var refreshRateHz = 0f
    private var started = false
    private val stages = FrameStageAccumulator()

    private val listener =
        Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            val durationNs = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
            if (durationNs <= 0) return@OnFrameMetricsAvailableListener
            val durationMs = durationNs / 1_000_000L
            frames++
            if (durationMs > 8L) over8MsFrames++
            if (durationMs > 16L) over16MsFrames++
            if (durationMs > 32L) {
                over32MsFrames++
                stages.add(durationMs, stageDurations(metrics))
            }
            if (durationMs > 700L) frozenFrames++
            maxDurationMs = maxOf(maxDurationMs, durationMs)
            if (frames % REPORT_INTERVAL == 0) report("sample")
        }

    fun start(window: Window) {
        if (started) return
        started = true
        refreshRateHz =
            runCatching {
                @Suppress("DEPRECATION")
                window.windowManager.defaultDisplay.refreshRate
            }.getOrDefault(0f)
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

    private fun stageDurations(metrics: FrameMetrics): LongArray {
        val ms = { id: Int -> metrics.getMetric(id) / 1_000_000L }
        return longArrayOf(
            ms(FrameMetrics.UNKNOWN_DELAY_DURATION),
            ms(FrameMetrics.INPUT_HANDLING_DURATION),
            ms(FrameMetrics.ANIMATION_DURATION),
            ms(FrameMetrics.LAYOUT_MEASURE_DURATION),
            ms(FrameMetrics.DRAW_DURATION),
            ms(FrameMetrics.SYNC_DURATION),
            ms(FrameMetrics.COMMAND_ISSUE_DURATION),
            ms(FrameMetrics.SWAP_BUFFERS_DURATION),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ms(FrameMetrics.GPU_DURATION) else 0L,
        )
    }

    private fun report(reason: String) {
        if (frames == 0) return
        AppLog.info(
            "Performance",
            "$screen frames ($reason): total=$frames over8ms=$over8MsFrames " +
                "over16ms=$over16MsFrames over32ms=$over32MsFrames " +
                "frozen=$frozenFrames max=${maxDurationMs}ms " +
                "refresh=${refreshRateHz.toInt()}Hz" + stages.describe(),
        )
    }

    private companion object {
        const val REPORT_INTERVAL = 600
    }
}
