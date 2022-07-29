package depollsoft.lib.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <reified T> preference(key: String, initialValue: T, crossinline afterSet: (T) -> Unit = {}): ReadWriteProperty<Any?, T> {
    Preferences.initialize(key, initialValue, T::class.java)
    return object : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = Preferences.get(key)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            Preferences.set(key, value)
            afterSet(value)
        }
    }
}

inline fun <reified T> writeThroughPreference(key: String, initialValue: T, crossinline afterSet: (T) -> Unit = {}): ReadWriteProperty<Any?, T> {
    val impl = preference(key, initialValue, afterSet)
    var memoized: T = initialValue
    var hasValue = false
    return object: ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            if (!hasValue) {
                memoized = impl.getValue(thisRef, property)
                hasValue = true
            }
            return memoized
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            impl.setValue(thisRef, property, value)
            memoized = value
        }
    }
}