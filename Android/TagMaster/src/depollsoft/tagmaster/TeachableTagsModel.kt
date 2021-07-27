package depollsoft.tagmaster

import com.bindroid.trackable.Trackable
import com.bindroid.trackable.TrackableCollection
import com.bindroid.trackable.Tracker
import com.bindroid.trackable.trackable
import com.parse.ParseUser
import depollsoft.lib.util.Preferences
import org.json.JSONArray
import org.json.JSONException

object TeachableTagsModel {
    private val model = ListModel("teachable")

    @JvmStatic
    var teachableTagIds: TrackableCollection<Int>
        get() = model.ids
        set(value) { model.ids = value }

    fun addTeachableTag(id: Int) = model.add(id)

    fun canMoveDown(id: Int): Boolean = model.canMoveDown(id)

    fun canMoveUp(id: Int): Boolean = model.canMoveUp(id)

    fun getIsTeachableTag(id: Int): Boolean = model.contains(id)

    fun moveDown(id: Int) = model.moveDown(id)

    fun moveUp(id: Int) = model.moveUp(id)

    fun removeTeachableTag(id: Int) = model.remove(id)

    fun resetTeachableTags() = model.reset()

    fun restoreFromUser() {
        if (ParseUser.getCurrentUser() != null) {
            val ids = ParseUser.getCurrentUser().getJSONArray("TeachableIds") ?: return
            val newIds = TrackableCollection<Int>()
            for (i in 0 until ids.length()) {
                try {
                    newIds.add(ids.getInt(i))
                } catch (e: JSONException) {
                }
            }
            teachableTagIds = newIds
        }
    }

    fun storeToUser() {
        if (ParseUser.getCurrentUser() != null) {
            try {
                val ids = JSONArray(teachableTagIds)
                ParseUser.getCurrentUser().put("TeachableIds", ids)
            } catch (e: Exception) {
            }
        }
    }
}