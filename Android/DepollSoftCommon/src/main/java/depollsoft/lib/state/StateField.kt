package depollsoft.lib.state

import androidx.compose.runtime.mutableStateOf
import kotlin.reflect.KProperty

/**
 * One observable value backed by Compose snapshot state, with the `get()`/`set()` shape the Java
 * models use. Setting an equal value is not a change, as with `mutableStateOf`'s default policy.
 *
 * Kotlin code can use it as a property delegate: `var name by StateField("")`.
 */
class StateField<T>(initialValue: T) {
    private val state = mutableStateOf(initialValue)

    fun get(): T = state.value

    fun set(value: T) {
        state.value = value
        SnapshotNotifications.ensureInstalled()
    }

    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): T = get()

    operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T,
    ) = set(value)

    override fun toString(): String = "${get()}"

    companion object {
        /** A field that starts out null, for Java callers: `StateField.empty()`. */
        @JvmStatic
        fun <T> empty(): StateField<T?> = StateField(null)
    }
}
