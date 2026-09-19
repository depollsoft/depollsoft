package depollsoft.pitchperfect

import android.graphics.Rect
import android.os.Looper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import depollsoft.lib.util.Preferences
import org.junit.Assert.assertTrue
import org.robolectric.Shadows.shadowOf

/**
 * Shared helpers for the Robolectric screen tests migrated from `src/androidTest`.
 *
 * Espresso's `isDisplayed()` means "attached, shown, and at least one pixel inside the screen".
 * Robolectric lays activities out for real once they reach RESUMED, so the same three conditions
 * are checkable directly and without Espresso's polling.
 */
internal object ScreenTestSupport {
    fun idle() = shadowOf(Looper.getMainLooper()).idle()

    fun isDisplayed(view: View?): Boolean =
        view != null && view.isShown && view.width > 0 && view.height > 0 &&
            view.getGlobalVisibleRect(Rect())

    fun assertDisplayed(
        name: String,
        view: View?,
    ) {
        assertTrue("$name should be displayed", isDisplayed(view))
    }

    /** Espresso's `scrollTo()`: bring the view inside its scrolling ancestor, then settle. */
    fun scrollTo(view: View) {
        view.requestRectangleOnScreen(Rect(0, 0, view.width, view.height), true)
        idle()
    }

    /**
     * Start each test from a never-launched app.
     *
     * `preference(key, default)` registers its default once, when the owning object is first
     * touched. Robolectric keeps statics alive between methods of one class, so a bare
     * [Preferences.clearTestValues] on the second method would drop defaults that will never be
     * registered again and leave the delegates returning null. Re-seeding the settings the screens
     * read keeps the cleared store consistent, and clearing the run-once keys restores the
     * first-launch state that suppresses the login and changelog prompts.
     */
    fun startFromFirstLaunch() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        seedSettingsDefaults()
        SongsModel.get().songLists = SongsModel.get().songLists
        forgetForeignDelegates()
    }

    /**
     * Re-register the settings a screen reads, after the in-memory store has been emptied.
     *
     * `preference(key, default)` registers its default exactly once, when the owning object is
     * first touched. Any test that empties the store afterwards leaves those delegates returning
     * null for the rest of the run, so whichever class happens to run next gets a
     * `NullPointerException` out of a plain settings read. Writing the production defaults back
     * makes that ordering stop mattering.
     */
    fun seedSettingsDefaults() {
        SettingsModel.toggleNotes = false
        SettingsModel.wakeLock = false
        SettingsModel.areAdsRemoved = false
    }

    /**
     * Drop activities other test classes left registered with `AppCompatDelegate`.
     *
     * `setDefaultNightMode` posts a recreate to every delegate it still knows about. A class that
     * builds an activity without destroying it leaves one behind, and the theme tests here would
     * then drive a recreate into that stale activity — surfacing as
     * `SavedStateRegistry was already restored` in a test that never touched it.
     */
    private fun forgetForeignDelegates() {
        for (field in androidx.appcompat.app.AppCompatDelegate::class.java.declaredFields) {
            if (!java.lang.reflect.Modifier.isStatic(field.modifiers)) continue
            runCatching {
                field.isAccessible = true
                when (val value = field.get(null)) {
                    is MutableCollection<*> -> value.clear()
                    is MutableMap<*, *> -> value.clear()
                    else -> Unit
                }
            }
        }
    }

    /**
     * Robolectric does not run `FirebaseInitProvider`, so the default app that the real process has
     * by the time any activity starts has to be created explicitly. It is built from the same
     * generated `google-services.json` resources the packaged app reads, so the signed-out
     * `Firebase.auth.currentUser == null` path the screens take is the production one.
     */
    /**
     * Undo a screen test without stranding work for the next one.
     *
     * Two things outlive a naive teardown. `AppCompatDelegate.setDefaultNightMode` posts a
     * recreate to every live activity, so the queue has to be drained *before* the controller is
     * closed or the recreate lands on a destroyed activity — and, worse, on the next test's fresh
     * activity, whose `SavedStateRegistry` has already been restored. And
     * `Preferences.setTestMode(false)` empties the in-memory store, which strands any class that
     * only calls `setTestMode(true)`: `preference(key, default)` registered its default once, at
     * object-init, and will never register it again. So the store is left seeded instead of empty.
     */
    fun finishScreenTest(controller: org.robolectric.android.controller.ActivityController<*>?) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        )
        idle()
        controller?.close()
        idle()
        startFromFirstLaunch()
    }

    fun ensureFirebaseApp() {
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        }
    }
}
