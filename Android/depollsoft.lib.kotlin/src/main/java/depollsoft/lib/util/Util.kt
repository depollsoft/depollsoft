package depollsoft.lib.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <reified T> preference(key: String, initialValue: T): ReadWriteProperty<Any?, T> {
    Preferences.initialize(key, initialValue, T::class.java)
    return object : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = Preferences.get(key)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            Preferences.set(key, value)
        }
    }
}