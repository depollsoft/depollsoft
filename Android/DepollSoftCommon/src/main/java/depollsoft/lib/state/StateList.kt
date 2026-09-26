package depollsoft.lib.state

import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * An observable list backed by a [SnapshotStateList].
 *
 * Reading anything from it (its size, an element, an iterator) makes the reader depend on the
 * whole list, and every mutation is a change. It keeps a public no-argument constructor because
 * [depollsoft.lib.json.JsonSerializer] rebuilds stored lists reflectively; the apps register it
 * under the aliases the Bindroid collection used to be stored as, so existing preferences load.
 */
class StateList<T> private constructor(
    private val items: SnapshotStateList<T>,
) : MutableList<T> by items,
    RandomAccess {
    constructor() : this(SnapshotStateList())

    constructor(initial: Collection<T>) : this(SnapshotStateList<T>().apply { addAll(initial) })

    /**
     * Replaces the contents as one change. [newItems] is copied first, so it may be this list or
     * a view of it.
     */
    fun replaceWith(newItems: Collection<T>) {
        val replacement = newItems.toList()
        batchStateChanges {
            items.clear()
            items.addAll(replacement)
        }
    }

    /** Applies [operation] as one change, the way a Bindroid collection transaction did. */
    fun transaction(operation: StateList<T>.() -> Unit) {
        batchStateChanges { operation() }
    }

    /** A plain copy of the current contents, safe to keep after the list changes. */
    fun snapshot(): List<T> = items.toList()

    override fun equals(other: Any?): Boolean = other is List<*> && items.toList() == other.toList()

    override fun hashCode(): Int = items.toList().hashCode()

    override fun toString(): String = items.toList().toString()
}
