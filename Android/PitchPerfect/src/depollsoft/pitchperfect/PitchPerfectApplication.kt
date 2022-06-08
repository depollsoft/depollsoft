package depollsoft.pitchperfect

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import com.bindroid.trackable.TrackableCollection
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.parse.*
import com.parse.facebook.ParseFacebookUtils
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.analytics.Analytics
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.*

class PitchPerfectApplication : RichApplication() {
    override fun onCreate() {
        super.onCreate()
        val isDebugSigned = false
        Note.setPlayer(object : Note.NotePlayer {
            override fun play(n: Note) {
                Note.DEFAULT_PLAYER.play(n)
                PitchPipeAppWidget.updateWidgets()
            }

            override fun stop(n: Note) {
                Note.DEFAULT_PLAYER.stop(n)
                PitchPipeAppWidget.updateWidgets()
            }
        })
        MobileAds.initialize(this)
        JsonSerializer.registerAlias(java.lang.Integer::class.java, "Integer")
        JsonSerializer.registerAlias(java.lang.Integer.TYPE, "int")
        JsonSerializer.registerAlias(Key::class.java, "Key")
        JsonSerializer.registerAlias(KeyType::class.java, "KeyType")
        JsonSerializer.registerAlias(Accidental::class.java, "Accidental")
        JsonSerializer.registerAlias(Note::class.java, "Note")
        JsonSerializer.registerAlias(PitchedSong::class.java, "PitchedSong")
        JsonSerializer.registerAlias(SongList::class.java, "SongList")
        JsonSerializer.registerAlias(String::class.java, "String")
        JsonSerializer.registerAlias(JsonSerializer.PRIMITIVE_CLASS, "Primitive")
        JsonSerializer.registerAlias(java.lang.Boolean::class.java, "Boolean")
        JsonSerializer.registerAlias(java.lang.Boolean.TYPE, "bool")
        JsonSerializer.registerAlias(java.lang.Double::class.java, "Double")
        JsonSerializer.registerAlias(java.lang.Double.TYPE, "double")
        JsonSerializer.registerAlias(TrackableCollection::class.java, "List")
        Parse.initialize(
            Parse.Configuration.Builder(this)
                .applicationId("cXYwcCUUP2f78OBfMlXu7dk03f2JRMQYXpCnv7H9")
                .clientKey("Y9ZIP3kLs1Jbh9Mpr2s8tRw9tjdGt6GuseuRHNdE")
                .server("https://pitchperfect-api.depollsoft.xyz")
                .build()
        )
        ParseFacebookUtils.initialize(this)

        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    private var userDoc: DocumentReference? = null

    private fun extraInit() {
        convertParseUser()

        var registration: ListenerRegistration? = null
        Firebase.auth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (registration != null) {
                registration!!.remove()
                SongsModel.get().detachFromFirestore()
                SettingsModel.detachFromFirestore()
            }
            if (user != null) {
                SongsModel.get().attachToFirestore()
                SettingsModel.attachToFirestore()
                userDoc = Firebase.firestore.document("users/${user.uid}")
                registration = userDoc!!.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("depollsoft.tagmaster", "Failed to listen to user document", error)
                    }
                }
            } else {
                userDoc = null
            }
        }
        val tags: MutableSet<String> = mutableSetOf()
        if (Firebase.auth.currentUser != null) {
            tags.add("logged_in")
        }
        Analytics.default.logEvent(Analytics.APP_OPEN, tags = tags)
    }

    fun convertParseUser() {
        val curUser = ParseUser.getCurrentUser()
        if (curUser != null) {
            Firebase.functions.getHttpsCallable("exchangeAuthToken").call().continueWith { t ->
                if (!t.isSuccessful) {
                    Log.e("depollsoft.tagmaster", "Token exchange error", t.exception)
                    return@continueWith
                }
                val dataDict = t.result.data as Map<String, Any?>
                val firebaseToken = dataDict["token"] as String
                Firebase.auth.signInWithCustomToken(firebaseToken).continueWith { t ->
                    if (!t.isSuccessful) {
                        Log.e(
                            "depollsoft.tagmaster",
                            "Failed to sign in with custom token",
                            t.exception
                        )
                        return@continueWith
                    }
                    ParseUser.logOut()
                    Log.d(
                        "depollsoft.tagmaster",
                        "Logged out Parse ${curUser.objectId} and logged in Firebase ${t.result!!.user!!.uid}"
                    )
                }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    protected override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    companion object {
        private const val DEBUG_SIGNATURE =
            "308201e53082014ea00302010202044f337962300d06092a864886f70d01010505003037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f6964204465627567301e170d3132303230393037343433345a170d3432303230313037343433345a3037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f696420446562756730819f300d06092a864886f70d010101050003818d0030818902818100b4d99aa8cff1bddc23c6313aebcfc93a869b8f918807e70e8b6ef03d722813b9090044ba2da31b53ae908393ee5fee0e2f6e582f9018bd6013dfef51bf073e049b42c8ba72387ca82687185603696c31c754669b0b3a84452190e87aac5800d7b07dce4475073f7a24cb0da8313d449d29803d3010296c588f3728033b3ddd1f0203010001300d06092a864886f70d0101050500038181002d0527ee1bb08ae465cc273b1b6fdafa4b1476d587712a8cdd47a7f64f3a790f2f7327c3c656da30af86a4c1febda7258a9226edb429365287c3b489dc74fe75160151543790903e3b5c0faac5bdb70192bbce9d41337426038d0061256b7e59abb9120291865ecc4adbc2b16428b0042120a937f5ef6744b2d4cba08e37b660"
        private const val FACEBOOK_DEBUG = "292538514135026"
        private const val FACEBOOK_PRODUCTION = "263872380333771"

        var themeMode: Int
            get() = Preferences.get("pitchperfect.theme")
                ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            set(value) {
                AppCompatDelegate.setDefaultNightMode(value)
                Preferences.set("pitchperfect.theme", value)
            }
    }
}