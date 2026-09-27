package depollsoft.tagmaster

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.junit.Assert.assertNull
import org.junit.rules.ExternalResource

/**
 * Keeps a device test's saved-list fixtures off the device owner's real lists.
 *
 * Refuses to run while someone is signed in, so nothing can reach their cloud copy; turns off
 * cloud storage for the test; and afterwards puts back both every stored preference and the lists
 * the running app holds in memory (each list's ids, and the custom-list registry), so a later test
 * or a later edit in the same process neither sees nor writes back the test's lists.
 */
class SavedListsSandbox : ExternalResource() {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val preferences
        get() = instrumentation.targetContext.getSharedPreferences("depollsoft.lib.Preferences", Context.MODE_PRIVATE)
    private var saved: Map<String, Any?>? = null
    private var savedLists: Map<String, List<Int>> = emptyMap()

    override fun before() {
        instrumentation.runOnMainSync {
            // Fail before touching anything. Never sign a real user out.
            assertNull("Saved-list device tests need a signed-out app", Firebase.auth.currentUser)
            ListModel.setTestMode(true)
            saved = HashMap(preferences.all)
            savedLists = listKeys().associateWith { ListModel(it).ids.toList() }
        }
    }

    override fun after() {
        val original = saved ?: return
        instrumentation.runOnMainSync {
            // Storage is still off, so these only reach this device's copy.
            (listKeys() + savedLists.keys).forEach { ListModel(it).ids = savedLists[it].orEmpty() }
            val editor = preferences.edit().clear()
            for ((key, value) in original) {
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
            editor.commit()
            // The registry reloads its names and order from the restored preferences.
            TagLists.resetForTest()
            ListModel.setTestMode(false)
        }
    }

    private fun listKeys(): Set<String> = ListModel.storedKeys() + TagLists.RESERVED + TagLists.customKeys
}
