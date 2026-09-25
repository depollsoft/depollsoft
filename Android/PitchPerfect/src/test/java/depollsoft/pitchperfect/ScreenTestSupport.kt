package depollsoft.pitchperfect

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Note
import org.robolectric.Shadows.shadowOf

/** Shared setup and teardown for the Robolectric screen tests. */
internal object ScreenTestSupport {
    fun idle() = shadowOf(Looper.getMainLooper()).idle()

    /** Plays nothing: screen tests are about state, and a sounding note would outlive its test. */
    val silentPlayer =
        object : Note.NotePlayer {
            override fun play(n: Note) = Unit

            override fun stop(n: Note) = Unit
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
        Note.setPlayer(silentPlayer)
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

    /**
     * Robolectric does not run `FirebaseInitProvider`, so the default app that the real process has
     * by the time any activity starts has to be created explicitly.
     *
     * It is deliberately built from synthetic options instead of the generated
     * `google-services.json` values. A Robolectric run once uploaded a
     * `PackageManager$NameNotFoundException`, thrown by firebase-sessions on a Firebase background
     * thread, to the *production* Crashlytics project: the manifest's
     * `firebase_crashlytics_collection_enabled=false` is read through the same package-manager
     * lookup that fails under Robolectric, so the SDK fell back to "collection enabled". Fake
     * credentials mean no unit test can address a real Firebase project even if that happens again,
     * and collection is additionally switched off explicitly so firebase-sessions never starts
     * publishing. `Firebase.auth.currentUser` is still null with these options, which is exactly the
     * signed-out path the screens take.
     */
    fun ensureFirebaseApp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context, UNIT_TEST_FIREBASE_OPTIONS)
        }
        // The nullable overload; the primitive-boolean one is deprecated.
        FirebaseApp.getInstance().setDataCollectionDefaultEnabled(false as Boolean?)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(false)
    }

    /** Well-formed but meaningless credentials: they belong to no Firebase project at all. */
    private val UNIT_TEST_FIREBASE_OPTIONS =
        FirebaseOptions.Builder()
            .setApplicationId("1:000000000000:android:0000000000000000")
            // Shaped like a real key (`A[\w-]{38}`) so Firebase Installations accepts it offline.
            .setApiKey("AIzaSyUnitTestUnitTestUnitTestUnitTest0")
            .setProjectId("pitchperfect-unit-test")
            .build()
}
