package depollsoft.lib.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@PublishedApi
internal fun initializePreference(key: String, initialValue: Any, clazz: Class<*>) {
    Preferences.initialize(key, initialValue, clazz)
}

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T> getPref(key: String): T = Preferences.get(key)

@PublishedApi
internal fun setPref(key: String, value: Any) {
    Preferences.set(key, value)
}

inline fun <reified T> preference(key: String, initialValue: T, crossinline afterSet: (T) -> Unit = {}): ReadWriteProperty<Any?, T> {
    initializePreference(key, initialValue as Any, T::class.java)
    return object : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = getPref(key)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            setPref(key, value as Any)
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