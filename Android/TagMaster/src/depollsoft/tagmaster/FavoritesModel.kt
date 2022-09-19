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
    private val model = ListModel("favorite")
    var favoriteIds: TrackableCollection<Int>
        get() = model.ids
        set(value) { model.ids = value }

    fun addFavorite(id: Int) = model.add(id)

    fun canMoveDown(id: Int): Boolean = model.canMoveDown(id)

    fun canMoveUp(id: Int): Boolean = model.canMoveUp(id)

    fun getIsFavorite(id: Int): Boolean = model.contains(id)

    fun moveDown(id: Int) = model.moveDown(id)

    fun moveUp(id: Int) = model.moveUp(id)

    fun removeFavorite(id: Int) = model.remove(id)

    fun resetFavorites() = model.reset()

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
}