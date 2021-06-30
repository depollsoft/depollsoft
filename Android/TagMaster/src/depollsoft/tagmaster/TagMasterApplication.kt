package depollsoft.tagmaster

import android.content.Context
import androidx.multidex.MultiDex
import bolts.Task
import com.bindroid.trackable.TrackableCollection
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.parse.Parse
import com.parse.facebook.ParseFacebookUtils
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.json.JsonSerializer
import depollsoft.tagmaster.barbershop.Tag
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class TagMasterApplication : RichApplication() {
    public var tagLoading: Task<Unit>? = null

    override fun onCreate() {
        super.onCreate()
        JsonSerializer.registerAlias(TrackableCollection::class.java,
                "depollsoft.lib.binding.ObservableCollection")
        Parse.initialize(Parse.Configuration.Builder(this)
                .server("https://tagmaster-api.depollsoft.xyz")
                .applicationId("RhfRllVEF5Qlm0DyVWzx6zi1yjxlmCrnqFtJFwbj")
                .clientKey("7xDIp24FCSz218vpiHhcudEb2Bytn8AzIrBfVLM4")
                .build())
        ParseFacebookUtils.initialize(this)

        var registration: ListenerRegistration? = null

        Firebase.auth.addAuthStateListener {
            registration?.remove()
            val user = it.currentUser
            if (user != null) {
                val userDoc = Firebase.firestore.document("users/${user.uid}")
                registration = userDoc.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        print(error)
                        return@addSnapshotListener
                    }
                    val oldTeachable = TeachableTagsModel.getTeachableTagIds()
                    val oldFavorites = FavoritesModel.getFavoriteIds()
                    if (snapshot!!.exists()) {
                        // There was no existing user, so initialize the user
                        userDoc.set(mapOf(
                                "lists" to mapOf(
                                        "favorite" to oldFavorites,
                                        "teachable" to oldTeachable
                                )
                        ), SetOptions.merge())
                    }

                    val teachableIds = (snapshot.get("lists.teachable") as? ArrayList<Int>) ?: oldTeachable
                    val favoriteIds = (snapshot.get("lists.favorite") as? ArrayList<Int>) ?: oldFavorites

                    // Don't try to write these back to the server -- they're already there
                    TeachableTagsModel.setTeachableTagIds(TrackableCollection(teachableIds))
                    FavoritesModel.setFavoriteIds(TrackableCollection(favoriteIds))

                    // Kick off precaching tags
                    GlobalScope.async {
                        tagLoading = Tag.queryByIds(teachableIds + favoriteIds, true)
                        tagLoading!!.await()
                        tagLoading = null
                    }
                }
            }
        }
    }

    protected override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    companion object {
        private const val DEBUG_SIGNATURE = "308201e53082014ea00302010202044f337962300d06092a864886f70d01010505003037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f6964204465627567301e170d3132303230393037343433345a170d3432303230313037343433345a3037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f696420446562756730819f300d06092a864886f70d010101050003818d0030818902818100b4d99aa8cff1bddc23c6313aebcfc93a869b8f918807e70e8b6ef03d722813b9090044ba2da31b53ae908393ee5fee0e2f6e582f9018bd6013dfef51bf073e049b42c8ba72387ca82687185603696c31c754669b0b3a84452190e87aac5800d7b07dce4475073f7a24cb0da8313d449d29803d3010296c588f3728033b3ddd1f0203010001300d06092a864886f70d0101050500038181002d0527ee1bb08ae465cc273b1b6fdafa4b1476d587712a8cdd47a7f64f3a790f2f7327c3c656da30af86a4c1febda7258a9226edb429365287c3b489dc74fe75160151543790903e3b5c0faac5bdb70192bbce9d41337426038d0061256b7e59abb9120291865ecc4adbc2b16428b0042120a937f5ef6744b2d4cba08e37b660"
        private const val FACEBOOK_DEBUG = "403828359632347"
        private const val FACEBOOK_PRODUCTION = "311400242255131"
    }
}