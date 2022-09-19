package depollsoft.tagmaster.barbershop

import com.bindroid.trackable.TrackableField
import com.bindroid.trackable.trackable

class TagQueryResult {
    var tags: List<Tag> by trackable(emptyList())
    var start: Int by trackable(0)
    var count: Int by trackable(0)
    var available: Int by trackable(0)
}