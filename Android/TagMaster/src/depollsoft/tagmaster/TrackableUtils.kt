package depollsoft.tagmaster

import com.bindroid.trackable.TrackableField
import kotlin.reflect.KProperty

operator fun <T> TrackableField<T>.getValue(
        thisRef: Any?,
        property: KProperty<*>
): T {
    return this.get()
}

operator fun <T> TrackableField<T>.setValue(
        thisRef: Any?,
        property: KProperty<*>,
        newValue: T) {
    this.set(newValue)
}