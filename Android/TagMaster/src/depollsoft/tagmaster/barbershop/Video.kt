package depollsoft.tagmaster.barbershop

import com.bindroid.trackable.*
import depollsoft.lib.xml.XmlElement
import java.text.SimpleDateFormat
import java.util.*

private fun parseDate(dateString: String?): Date {
    if (dateString.isNullOrBlank()) return Date(0)
    return try {
        Date(dateString.toLong())
    } catch (e: NumberFormatException) {
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateString) ?: Date(0)
        } catch (e2: Exception) {
            try {
                SimpleDateFormat("MMM d, yyyy", Locale.US).parse(dateString) ?: Date(0)
            } catch (e3: Exception) {
                Date(0)
            }
        }
    }
}

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