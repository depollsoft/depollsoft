package depollsoft.lib.state

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot

/**
 * The app models keep their state in Compose snapshot state (`mutableStateOf`,
 * `mutableStateListOf`), so composables recompose when it changes. This file covers what snapshot
 * state does not give for free: a hand-raised change signal for state that lives outside the
 * snapshot system (SharedPreferences, the package manager), and applying several writes as one.
 */

/**
 * A change signal for state stored outside the snapshot system.
 *
 * Call [read] wherever the external value is read and [changed] whenever it changes; anything
 * that read it (a composable, say) runs again.
 */
class ChangeSignal {
    private val version = mutableIntStateOf(0)

    fun read() {
        version.intValue
    }

    fun changed() {
        Snapshot.withMutableSnapshot { version.intValue++ }
    }
}

/** Applies every write in [block] as one change, the way a Bindroid collection transaction did. */
inline fun <R> batchStateChanges(block: () -> R): R = Snapshot.withMutableSnapshot(block)
