package depollsoft.tagmaster.barbershop

import depollsoft.lib.xml.XmlElement
import depollsoft.tagmaster.parseDate
import java.util.*

class Video {
    var id: Int = 0
    var description: String? = null
    var sungKey: String? = null
    var isMultitrack: Boolean = false
    var youTubeCode: String? = null
    var sungBy: String? = null
    var sungWebsite: String? = null
    var posted: Date? = null

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
