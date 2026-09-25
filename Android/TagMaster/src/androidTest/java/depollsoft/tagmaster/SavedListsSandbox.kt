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
 * cloud storage for the test; and puts every stored preference back afterwards, including the
 * favourites, Teachable Tags and custom lists the test replaced.
 */
class SavedListsSandbox : ExternalResource() {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val preferences
        get() = instrumentation.targetContext.getSharedPreferences("depollsoft.lib.Preferences", Context.MODE_PRIVATE)
    private var saved: Map<String, Any?>? = null

    override fun before() {
        instrumentation.runOnMainSync {
            // Fail before touching anything. Never sign a real user out.
            assertNull("Saved-list device tests need a signed-out app", Firebase.auth.currentUser)
            ListModel.setTestMode(true)
            saved = HashMap(preferences.all)
        }
    }

    override fun after() {
        val original = saved ?: return
        instrumentation.runOnMainSync {
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
            ListModel.setTestMode(false)
        }
    }
}
