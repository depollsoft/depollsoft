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
    private const val TeachableTagsPreference = "tagmaster.TeachableTags"

    @JvmStatic
    var teachableTagIds: TrackableCollection<Int>
            by trackable(Preferences.get(TeachableTagsPreference) ?: TrackableCollection())

    fun addTeachableTag(id: Int) {
        if (!teachableTagIds.contains(id)) teachableTagIds.add(id)
    }

    fun canMoveDown(id: Int): Boolean {
        val index: Int = teachableTagIds.indexOf(id)
        return index < teachableTagIds.size - 1
    }

    fun canMoveUp(id: Int): Boolean {
        val index: Int = teachableTagIds.indexOf(id)
        return index > 0
    }

    fun getIsTeachableTag(id: Int): Boolean {
        return teachableTagIds.contains(id)
    }

    fun moveDown(id: Int) {
        val index: Int = teachableTagIds.indexOf(id)
        teachableTagIds.removeAt(index)
        teachableTagIds.add(index + 1, id)
    }

    fun moveUp(id: Int) {
        val index: Int = teachableTagIds.indexOf(id)
        teachableTagIds.removeAt(index)
        teachableTagIds.add(index - 1, id)
    }

    fun removeTeachableTag(id: Int) {
        teachableTagIds.remove(Integer.valueOf(id))
    }

    fun resetTeachableTags() {
        teachableTagIds.clear()
    }

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

    private fun storeValue() {
        Preferences.set(
            TeachableTagsPreference,
            teachableTagIds
        )
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
                Trackable.track(this, { teachableTagIds.track() })
            }
        }, { teachableTagIds.track() })
        ListModel("teachable")
    }
}