package depollsoft.tagmaster.barbershop

import com.bindroid.trackable.*

class RemoteLocation {
    var uri by TrackableField<String>()
    var type by TrackableField<String>()
}