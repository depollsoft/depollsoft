package depollsoft.lib.state

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lets tests watch model state from plain Kotlin, the way a composable does: [watchState] runs a
 * callback when state it read changes. The apps observe state only through Compose, which sends
 * snapshot notifications itself once anything composes, so this lives with the tests.
 */

/** A handle that stops a [watchState] from running again. */
fun interface StateWatch {
    fun stop()
}

/**
 * Runs [onChange] with the value of [read] every time snapshot state that [read] used changes.
 *
 * [read] runs immediately; [onChange] runs on the main thread after each later change, never for
 * the initial value unless [emitInitial] is set. Changes are coalesced: several writes before the
 * next frame of the main looper produce one call with the latest value.
 */
fun <T> watchState(
    emitInitial: Boolean = false,
    read: () -> T,
    onChange: (T) -> Unit,
): StateWatch {
    SnapshotNotifications.ensureInstalled()
    val scope = Any()
    val stopped = AtomicBoolean(false)
    lateinit var observe: () -> Unit
    val onValueChanged: (Any) -> Unit = {
        if (!stopped.get()) observe()
    }
    var first = true
    observe = {
        // State created since the global snapshot last advanced reports no writes until it is
        // marked initialized; the Recomposer does this for composables, so do it for watchers.
        Snapshot.notifyObjectsInitialized()
        var value: Any? = null
        SnapshotNotifications.observer.observeReads(scope, onValueChanged) { value = read() }
        if (!first || emitInitial) {
            @Suppress("UNCHECKED_CAST")
            onChange(value as T)
        }
        first = false
    }
    SnapshotNotifications.runOnMain(observe)
    return StateWatch {
        stopped.set(true)
        SnapshotNotifications.runOnMain { SnapshotNotifications.observer.clear(scope) }
    }
}

/**
 * Makes snapshot writes made outside a composition reach [watchState]: the job Compose UI's global
 * snapshot manager does in the apps. Installing it twice is harmless.
 */
object SnapshotNotifications {
    /** Null on a plain JVM, where there is no main looper to post to. */
    private val main: Handler? by lazy {
        runCatching { Looper.getMainLooper()?.let(::Handler) }.getOrNull()
    }
    private val installed = AtomicBoolean(false)
    private val pending = AtomicBoolean(false)

    internal val observer: SnapshotStateObserver by lazy {
        SnapshotStateObserver { task -> runOnMain(task) }.also { it.start() }
    }

    fun ensureInstalled() {
        if (!installed.compareAndSet(false, true)) return
        Snapshot.registerGlobalWriteObserver {
            val handler = main ?: return@registerGlobalWriteObserver
            if (pending.compareAndSet(false, true)) {
                handler.post {
                    pending.set(false)
                    Snapshot.sendApplyNotifications()
                }
            }
        }
    }

    /** Delivers pending changes now, instead of waiting for the main looper. */
    fun flush() {
        Snapshot.sendApplyNotifications()
    }

    internal fun runOnMain(task: () -> Unit) {
        val handler = main
        if (handler == null || Looper.myLooper() == handler.looper) task() else handler.post(task)
    }
}
