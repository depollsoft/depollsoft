package depollsoft.tagmaster.barbershop

import com.bindroid.trackable.*
import depollsoft.lib.xml.XmlElement
import depollsoft.tagmaster.parseDate
import java.util.*

class Video {
    var id by TrackableInt()
    var description by TrackableField<String?>()
    var sungKey by TrackableField<String?>()
    var isMultitrack by TrackableBoolean()
    var youTubeCode by TrackableField<String?>()
    var sungBy by TrackableField<String?>()
    var sungWebsite by TrackableField<String?>()
    var posted by TrackableField<Date>()

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