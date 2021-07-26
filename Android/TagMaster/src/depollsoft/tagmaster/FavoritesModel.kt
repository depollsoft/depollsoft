package depollsoft.tagmaster

import android.os.Handler
import android.os.Looper
import com.bindroid.trackable.Trackable
import com.bindroid.trackable.TrackableCollection
import com.bindroid.trackable.Tracker
import com.bindroid.trackable.trackable
import com.parse.ParseUser
import depollsoft.lib.util.Preferences
import org.json.JSONArray
import org.json.JSONException

object FavoritesModel {
    private const val FavoritesPreference = "tagmaster.Favorites"
    var favoriteIds: TrackableCollection<Int>
            by trackable(Preferences.get(FavoritesPreference) ?: TrackableCollection())

    fun addFavorite(id: Int) {
        if (!favoriteIds.contains(id)) favoriteIds.add(id)
    }

    fun canMoveDown(id: Int): Boolean {
        val index: Int = favoriteIds.indexOf(id)
        return index < favoriteIds.size - 1
    }

    fun canMoveUp(id: Int): Boolean {
        val index: Int = favoriteIds.indexOf(id)
        return index > 0
    }

    fun getIsFavorite(id: Int): Boolean {
        return favoriteIds.contains(id)
    }

    fun moveDown(id: Int) {
        val index: Int = favoriteIds.indexOf(id)
        favoriteIds.removeAt(index)
        favoriteIds.add(index + 1, id)
    }

    fun moveUp(id: Int) {
        val index: Int = favoriteIds.indexOf(id)
        favoriteIds.removeAt(index)
        favoriteIds.add(index - 1, id)
    }

    fun removeFavorite(id: Int) {
        favoriteIds.remove(Integer.valueOf(id))
    }

    fun resetFavorites() {
        favoriteIds.clear()
    }

    fun restoreFromUser() {
        if (ParseUser.getCurrentUser() != null) {
            val ids = ParseUser.getCurrentUser().getJSONArray("FavoriteIds") ?: return
            val newIds = TrackableCollection<Int>()
            for (i in 0 until ids.length()) {
                try {
                    newIds.add(ids.getInt(i))
                } catch (e: JSONException) {
                }
            }
            favoriteIds = newIds
        }
    }

    fun storeToUser() {
        if (ParseUser.getCurrentUser() != null) {
            try {
                val ids = JSONArray(favoriteIds)
                try {
                    ParseUser.getCurrentUser().put("FavoriteIds", ids)
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).postDelayed({ storeToUser() }, 100)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun storeValue() {
        Preferences.set(FavoritesPreference, favoriteIds)
        return
        if (ParseUser.getCurrentUser() != null) {
            storeToUser()
            ParseUser.getCurrentUser().saveEventually()
        }
    }

    init {
        Trackable.track(object : Tracker {
            override fun update() {
                storeValue()
                Trackable.track(this, { favoriteIds.track() })
            }
        }, { favoriteIds.track() })
    }
}