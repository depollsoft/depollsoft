package depollsoft.tagmaster.barbershop

import depollsoft.lib.state.StateField
import depollsoft.lib.xml.XmlElement
import depollsoft.tagmaster.parseDate
import java.util.*

class Video {
    var id: Int by StateField(0)
    var description: String? by StateField(null)
    var sungKey: String? by StateField(null)
    var isMultitrack: Boolean by StateField(false)
    var youTubeCode: String? by StateField(null)
    var sungBy: String? by StateField(null)
    var sungWebsite: String? by StateField(null)
    var posted: Date? by StateField(null)

    fun parseFromXml(elem: XmlElement) {
        for (property in elem.elements) {
            var propValue = property.value
            if (propValue == null || propValue.trim { it <= ' ' }.isEmpty()) propValue = null
            if (propValue != null) propValue = propValue.trim { it <= ' ' }
            when (property.name) {
                "id" -> id = propValue!!.toInt()
                "Desc" -> description = propValue
                "SungKey" -> sungKey = propValue
                "Multitrack" -> isMultitrack = "Yes" == propValue
                "Code" -> youTubeCode = propValue
                "SungBy" -> sungBy = propValue
                "SungWebsite" -> sungWebsite = propValue
                "Posted" -> posted = parseDate(propValue)
            }
        }
    }
}
