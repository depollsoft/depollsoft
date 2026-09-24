package depollsoft.tagmaster.barbershop

import depollsoft.lib.state.StateField

class RemoteLocation {
    var uri: String? by StateField(null)
    var type: String? by StateField(null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteLocation) return false
        return uri == other.uri && type == other.type
    }

    override fun hashCode(): Int {
        var result = uri?.hashCode() ?: 0
        result = 31 * result + (type?.hashCode() ?: 0)
        return result
    }
}
